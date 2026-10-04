package sms.communication.gateway;

import com.sun.net.httpserver.*;
import sms.system_services.data.DatabaseManager;
import sms.system_services.events.*;
import sms.observability.AuditLogger;
import sms.domain.analytics.StudentRiskAnalyzer;
import sms.domain.attendance.AttendanceAlertEngine;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class WebServer implements sms.system_services.events.EventListener {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final CopyOnWriteArrayList<OutputStream> clients = new CopyOnWriteArrayList<>();
    private final Path projectRoot = Paths.get("").toAbsolutePath().normalize();
    private HttpServer server;

    public void stop() { if(server != null) server.stop(0); }
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::serveIndex);
        server.createContext("/api/", this::api);
        server.createContext("/api/events", this::sse);
        EventBus.getInstance().subscribe(Object.class, this);
        server.start();
        emitGeneric("SYSTEM_ONLINE", "SYSTEM");
        System.out.println("[WebServer] Academic System Environment: http://localhost:" + port);
    }

    private void serveIndex(HttpExchange x) throws IOException {
        if (!"/".equals(x.getRequestURI().getPath())) { send(x,404,"Not Found","text/plain"); return; }
        Path file=projectRoot.resolve("web/index.html");
        if(!Files.exists(file)){send(x,500,"web/index.html not found","text/plain");return;}
        byte[] b=Files.readAllBytes(file); x.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");
        x.sendResponseHeaders(200,b.length);try(OutputStream o=x.getResponseBody()){o.write(b);}
    }

    private void sse(HttpExchange x) throws IOException {
        x.getResponseHeaders().set("Content-Type","text/event-stream; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control","no-cache");
        x.getResponseHeaders().set("Connection","keep-alive");
        x.sendResponseHeaders(200,0);
        OutputStream o=x.getResponseBody(); clients.add(o);
        try { o.write(("data: "+json(Map.of("type","CONNECTED","time",Instant.now().toString()))+"\n\n").getBytes(StandardCharsets.UTF_8)); o.flush();
              while(true) { Thread.sleep(30000); o.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8)); o.flush(); } }
        catch(Exception ignored) { clients.remove(o); try{o.close();}catch(Exception e){} }
    }

    @Override public void onEvent(Object event) {
        String type=event.getClass().getSimpleName();
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("type",type);payload.put("time",Instant.now().toString());
        if(event instanceof MarkUpdatedEvent m){payload.put("studentId",m.studentId);payload.put("subject",m.subjectCode);payload.put("mark",m.newMark);}
        if(event instanceof AttendanceChangedEvent a){payload.put("studentId",a.studentId);payload.put("attendance",a.currentPercentage);}
        db.executeUpdate("INSERT INTO system_events(timestamp,type,student_id,payload) VALUES(CURRENT_TIMESTAMP,?,?,?)",
                type, studentId(event), json(payload));
        String msg="data: "+json(payload)+"\n\n";
        for(OutputStream o:clients) try{o.write(msg.getBytes(StandardCharsets.UTF_8));o.flush();}catch(Exception e){clients.remove(o);}
    }
    private String studentId(Object e){if(e instanceof MarkUpdatedEvent)return ((MarkUpdatedEvent)e).studentId;if(e instanceof AttendanceChangedEvent)return ((AttendanceChangedEvent)e).studentId;return null;}

    private void api(HttpExchange x) throws IOException {
        String path=x.getRequestURI().getPath().substring("/api/".length());
        String method=x.getRequestMethod();
        try {
            if(path.equals("overview")) {sendJson(x,overview());return;}
            if(path.equals("students") && method.equals("GET")) {sendJson(x,students(query(x,"q")));return;}
            if(path.equals("students") && method.equals("POST")) {createStudent(x);return;}
            if(path.startsWith("students/")) {studentRoute(x,path.substring(9),method);return;}
            if(path.equals("classes") && method.equals("GET")) {sendJson(x,db.query("SELECT * FROM classes ORDER BY name"));return;}
            if(path.equals("classes") && method.equals("POST")) {Map<String,String> f=form(x);db.addClass(req(f,"name"),f.getOrDefault("department",""),Integer.parseInt(f.getOrDefault("semester","1")),f.getOrDefault("room",""),Integer.parseInt(f.getOrDefault("capacity","30")));emitGeneric("CLASS_CREATED","");sendJson(x,Map.of("ok",true),201);return;}
            if(path.equals("subjects") && method.equals("GET")) {sendJson(x,db.query("SELECT * FROM subjects ORDER BY code"));return;}
            if(path.equals("subjects") && method.equals("POST")) {Map<String,String> f=form(x);db.addSubject(req(f,"code"),req(f,"name"),Integer.parseInt(f.getOrDefault("credits","3")));emitGeneric("SUBJECT_CREATED","");sendJson(x,Map.of("ok",true),201);return;}
            if(path.equals("analytics")) {sendJson(x,analytics());return;}
            if(path.equals("risk")) {sendJson(x,risk());return;}
            if(path.equals("alerts")) {sendJson(x,alerts());return;}
            if(path.equals("database")) {sendJson(x,databaseView());return;}
            if(path.equals("event-log")) {sendJson(x,db.query("SELECT * FROM system_events ORDER BY id DESC LIMIT 100"));return;}
            if(path.equals("audit")) {sendJson(x,db.query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 100"));return;}
            if(path.equals("runtime")) {sendJson(x,runtime());return;}
            if(path.equals("health")) {sendJson(x,health());return;}
            if(path.equals("build") && method.equals("POST")) {sendJson(x,build());return;}
            if(path.equals("files")) {sendJson(x,files(query(x,"path")));return;}
            if(path.equals("file")) {sendFileContent(x,query(x,"path"));return;}
            if(path.equals("ai")) {sendJson(x,aiStatus());return;}
            if(path.equals("artifacts")) {sendJson(x,artifacts());return;}
            sendJson(x,Map.of("error","Unknown API route", "path",path),404);
        } catch(Exception e) {
            e.printStackTrace(); sendJson(x,Map.of("error",e.getMessage()),500);
        }
    }

    private void studentRoute(HttpExchange x,String rest,String method) throws Exception {
        String[] p=rest.split("/");
        String id=URLDecoder.decode(p[0],StandardCharsets.UTF_8);
        if(p.length==1 && method.equals("GET")) {sendJson(x,student(id));return;}
        if(p.length==1 && method.equals("PUT")) {updateStudent(x,id);return;}
        if(p.length==1 && method.equals("DELETE")) {db.deleteStudent(id);audit("DELETE_STUDENT",id,"Student removed");emitGeneric("STUDENT_DELETED",id);sendJson(x,Map.of("ok",true));return;}
        if(p.length==2 && p[1].equals("marks") && method.equals("POST")) {updateMark(x,id);return;}
        if(p.length==2 && p[1].equals("attendance") && method.equals("POST")) {recordAttendance(x,id);return;}
        sendJson(x,Map.of("error","Unknown student route"),404);
    }

    private void createStudent(HttpExchange x) throws Exception {
        Map<String,String> f=form(x);
        String id=req(f,"id"),name=req(f,"name");
        db.addStudent(id,name,f.getOrDefault("email",""),f.getOrDefault("phone",""),f.getOrDefault("department",""),Integer.parseInt(f.getOrDefault("semester","1")),f.getOrDefault("type","UNDERGRADUATE"),f.getOrDefault("status","ACTIVE"));
        audit("CREATE_STUDENT",id,name);emitGeneric("STUDENT_CREATED",id);sendJson(x,Map.of("ok",true,"id",id),201);
    }
    private void updateStudent(HttpExchange x,String id) throws Exception {
        Map<String,String> f=form(x);
        db.updateStudent(id,f.getOrDefault("name",""),f.getOrDefault("email",""),f.getOrDefault("phone",""),f.getOrDefault("department",""),Integer.parseInt(f.getOrDefault("semester","1")),f.getOrDefault("status","ACTIVE"));
        audit("UPDATE_STUDENT",id,f.getOrDefault("name",""));emitGeneric("STUDENT_UPDATED",id);sendJson(x,student(id));
    }
    private void updateMark(HttpExchange x,String id) throws Exception {
        Map<String,String> f=form(x);double mark=Double.parseDouble(req(f,"mark"));if(mark<0||mark>100)throw new IllegalArgumentException("Mark must be 0-100");
        String subject=req(f,"subject");db.upsertMark(id,subject,mark);
        EventBus.getInstance().publish(new MarkUpdatedEvent(id,subject,mark));audit("UPDATE_MARK",id,subject+" = "+mark);
        sendJson(x,student(id));
    }
    private void recordAttendance(HttpExchange x,String id) throws Exception {
        Map<String,String> f=form(x);String subject=req(f,"subject");boolean present=Boolean.parseBoolean(f.getOrDefault("present","true"));
        db.recordAttendance(id,subject,present);double pct=db.attendance(id);
        EventBus.getInstance().publish(new AttendanceChangedEvent(id,pct));audit("RECORD_ATTENDANCE",id,subject+" = "+(present?"Present":"Absent"));
        sendJson(x,student(id));
    }

    private Map<String,Object> overview() throws Exception {
        int count=((Number)db.query("SELECT COUNT(*) AS n FROM students").get(0).get("n")).intValue();
        int risks=0,att=0;for(Map<String,Object>s:db.query("SELECT * FROM students")){double g=gpa((String)s.get("id"));double a=((Number)s.get("attendance")).doubleValue();if(g<6)risks++;if(a<75)att++;}
        return new LinkedHashMap<>(Map.of("students",count,"academicRisks",risks,"attendanceRisks",att,"components",componentCount(),"recent",db.query("SELECT * FROM system_events ORDER BY id DESC LIMIT 8")));
    }
    private int componentCount(){int c=0;try{c=(int)Files.walk(projectRoot.resolve("src")).filter(p->p.toString().endsWith(".java")).count();}catch(Exception ignored){}return c;}
    private List<Map<String,Object>> students(String q) throws Exception {
        if(q==null||q.isBlank())return db.query("SELECT * FROM students ORDER BY name");
        String s="%"+q.toLowerCase()+"%";return db.query("SELECT * FROM students WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? OR LOWER(department) LIKE ? ORDER BY name",s,s,s);
    }
    private Map<String,Object> student(String id) throws Exception {
        List<Map<String,Object>> rows=db.query("SELECT * FROM students WHERE id=?",id);if(rows.isEmpty())throw new IllegalArgumentException("Student not found");
        Map<String,Object> s=new LinkedHashMap<>(rows.get(0));s.put("gpa",gpa(id));s.put("risk",riskLevel(id));
        s.put("marks",db.query("SELECT m.subject_code,m.mark,sub.name,sub.credits FROM marks m LEFT JOIN subjects sub ON sub.code=m.subject_code WHERE m.student_id=? ORDER BY m.subject_code",id));
        s.put("attendanceRecords",db.query("SELECT subject_code,present,recorded_at FROM attendance WHERE student_id=? ORDER BY id DESC LIMIT 30",id));
        s.put("events",db.query("SELECT * FROM system_events WHERE student_id=? ORDER BY id DESC LIMIT 12",id));return s;
    }
    private double gpa(String id)throws Exception{List<Map<String,Object>> r=db.query("SELECT AVG(mark) AS avg_mark FROM marks WHERE student_id=?",id);double avg=r.isEmpty()||r.get(0).get("avg_mark")==null?0:((Number)r.get(0).get("avg_mark")).doubleValue();return Math.round((avg/10.0)*100.0)/100.0;}
    private String riskLevel(String id)throws Exception{double g=gpa(id),a=((Number)db.query("SELECT attendance FROM students WHERE id=?",id).get(0).get("attendance")).doubleValue();return g<5||a<65?"HIGH":g<6.5||a<75?"MEDIUM":"LOW";}
    private Map<String,Object> analytics()throws Exception{
        List<Map<String,Object>> s=db.query("SELECT department,COUNT(*) AS student_count,AVG(attendance) AS avg_attendance FROM students GROUP BY department ORDER BY department");
        return Map.of("departments",s,"averageGpa",averageGpa(),"averageAttendance",averageAttendance(),"gradeBands",gradeBands());
    }
    private double averageGpa()throws Exception{List<Map<String,Object>> s=db.query("SELECT id FROM students");double t=0;for(Map<String,Object>r:s)t+=gpa((String)r.get("id"));return s.isEmpty()?0:Math.round(t/s.size()*100)/100.0;}
    private double averageAttendance()throws Exception{List<Map<String,Object>>r=db.query("SELECT AVG(attendance) a FROM students");return Math.round(((Number)r.get(0).get("a")).doubleValue()*100)/100.0;}
    private Map<String,Object> gradeBands()throws Exception{Map<String,Object>m=new LinkedHashMap<>();m.put("Excellent",db.query("SELECT COUNT(*) AS n FROM marks WHERE mark>=85").get(0).get("n"));m.put("Good",db.query("SELECT COUNT(*) AS n FROM marks WHERE mark>=70 AND mark<85").get(0).get("n"));m.put("Needs Attention",db.query("SELECT COUNT(*) AS n FROM marks WHERE mark<70").get(0).get("n"));return m;}
    private List<Map<String,Object>> risk()throws Exception{List<Map<String,Object>>out=new ArrayList<>();for(Map<String,Object>s:db.query("SELECT id,name,department,attendance FROM students")){Map<String,Object>r=new LinkedHashMap<>(s);r.put("gpa",gpa((String)s.get("id")));r.put("risk",riskLevel((String)s.get("id")));if(!"LOW".equals(r.get("risk")))out.add(r);}return out;}
    private List<Map<String,Object>> alerts()throws Exception{return db.query("SELECT * FROM system_events WHERE type LIKE '%RISK%' OR type LIKE '%ATTENDANCE%' ORDER BY id DESC LIMIT 50");}
    private Map<String,Object> databaseView()throws Exception{return Map.of("engine","HSQLDB","mode","file-backed embedded database","path",projectRoot.resolve("data/academic").toString(),"tables",List.of("students","classes","subjects","marks","attendance","audit_logs","system_events"),"students",db.query("SELECT * FROM students ORDER BY id"),"subjects",db.query("SELECT * FROM subjects ORDER BY code"),"classes",db.query("SELECT * FROM classes ORDER BY id"),"marks",db.query("SELECT * FROM marks ORDER BY id")); }
    private Map<String,Object> runtime(){return new LinkedHashMap<>(Map.of("runtime","RUNNING","webGateway","CONNECTED","eventBus","ACTIVE","database","CONNECTED","filesystemWatcher","ACTIVE","scheduler","READY","components",componentCount(),"java",System.getProperty("java.version"),"workspace",projectRoot.toString()));}
    private Map<String,Object> health(){return Map.of("status","HEALTHY","database",true,"api",true,"sse",true,"filesystem",Files.exists(projectRoot.resolve("src")));}
    private Map<String,Object> aiStatus(){return Map.of("mode","CONTROLLED DEVELOPMENT","observe","READY","propose","READY","test","READY","apply","APPROVAL REQUIRED","note","AI changes must be validated before entering the live codespace.");}
    private Map<String,Object> artifacts(){return Map.of("exports",Files.exists(projectRoot.resolve("exports")),"backups",Files.exists(projectRoot.resolve("backups")),"data",Files.exists(projectRoot.resolve("data")));}

    private Map<String,Object> build() {
        try {
            Path src=projectRoot.resolve("src"), bin=projectRoot.resolve("bin");
            Files.createDirectories(bin);
            List<String> files=new ArrayList<>();
            try(var st=Files.walk(src)){st.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toString()));}
            List<String> cmd=new ArrayList<>(List.of("javac","-cp",projectRoot.resolve("lib/hsqldb.jar").toString(),"-d",bin.toString()));
            cmd.addAll(files);
            Process p=new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
            int code=p.waitFor();
            return Map.of("success",code==0,"exitCode",code,"files",files.size(),"output",out);
        } catch(Exception e) { return Map.of("success",false,"error",e.toString()); }
    }

    private List<Map<String,Object>> files(String rel)throws IOException{
        Path base=projectRoot.resolve("src").normalize();Path p=rel==null||rel.isBlank()?base:base.resolve(rel).normalize();
        if(!p.startsWith(base))throw new SecurityException("Path outside workspace");
        List<Map<String,Object>> out=new ArrayList<>();if(Files.isDirectory(p))try(var st=Files.list(p)){st.sorted().forEach(x->{Map<String,Object>m=new LinkedHashMap<>();m.put("name",x.getFileName().toString());m.put("directory",Files.isDirectory(x));m.put("path",base.relativize(x).toString().replace('\\','/'));try{m.put("size",Files.isDirectory(x)?0:Files.size(x));m.put("modified",Files.getLastModifiedTime(x).toString());}catch(Exception e){}out.add(m);});}return out;
    }
    private void sendFileContent(HttpExchange x,String rel)throws IOException{
        Path base=projectRoot.resolve("src").normalize();Path p=base.resolve(rel==null?"":rel).normalize();if(!p.startsWith(base)||!Files.isRegularFile(p))throw new FileNotFoundException("File not found");
        String content=Files.readString(p);sendJson(x,Map.of("path",base.relativize(p).toString().replace('\\','/'),"content",content,"modified",Files.getLastModifiedTime(p).toString()));
    }

    private Map<String,String> form(HttpExchange x)throws IOException{String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);Map<String,String>m=new HashMap<>();for(String pair:body.split("&")){if(pair.isBlank())continue;String[]kv=pair.split("=",2);m.put(URLDecoder.decode(kv[0],StandardCharsets.UTF_8),URLDecoder.decode(kv.length>1?kv[1]:"",StandardCharsets.UTF_8));}return m;}
    private String query(HttpExchange x,String k){String q=x.getRequestURI().getRawQuery();if(q==null)return "";for(String p:q.split("&")){String[]a=p.split("=",2);if(a[0].equals(k))return a.length>1?URLDecoder.decode(a[1],StandardCharsets.UTF_8):"";}return "";}
    private String req(Map<String,String>m,String k){String v=m.get(k);if(v==null||v.isBlank())throw new IllegalArgumentException(k+" is required");return v;}
    private void audit(String action,String id,String details){db.executeUpdate("INSERT INTO audit_logs(timestamp,actor,action,details) VALUES(CURRENT_TIMESTAMP,?,?,?)","Admin",action,id+" â€” "+details);}
    private void emitGeneric(String type,String id){Map<String,Object>m=Map.of("type",type,"studentId",id,"time",Instant.now().toString());db.executeUpdate("INSERT INTO system_events(timestamp,type,student_id,payload) VALUES(CURRENT_TIMESTAMP,?,?,?)",type,id,json(m));broadcast(m);}
    private void broadcast(Map<String,Object>m){String msg="data: "+json(m)+"\n\n";for(OutputStream o:clients)try{o.write(msg.getBytes(StandardCharsets.UTF_8));o.flush();}catch(Exception e){clients.remove(o);}}
    private void sendJson(HttpExchange x,Object o)throws IOException{sendJson(x,o,200);}private void sendJson(HttpExchange x,Object o,int code)throws IOException{send(x,code,json(o),"application/json; charset=utf-8");}
    private void send(HttpExchange x,int code,String body,String type)throws IOException{byte[]b=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(code,b.length);try(OutputStream o=x.getResponseBody()){o.write(b);}}
    @SuppressWarnings("unchecked") private String json(Object o){
        if(o==null)return"null";if(o instanceof String||o instanceof Character)return"\""+esc(String.valueOf(o))+"\"";if(o instanceof Number||o instanceof Boolean)return String.valueOf(o);
        if(o instanceof Map){StringBuilder b=new StringBuilder("{");boolean first=true;for(var e:((Map<String,Object>)o).entrySet()){if(!first)b.append(',');first=false;b.append(json(e.getKey())).append(':').append(json(e.getValue()));}return b.append('}').toString();}
        if(o instanceof Iterable){StringBuilder b=new StringBuilder("[");boolean first=true;for(Object v:(Iterable<?>)o){if(!first)b.append(',');first=false;b.append(json(v));}return b.append(']').toString();}
        return json(String.valueOf(o));
    }
    private String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");}
}

