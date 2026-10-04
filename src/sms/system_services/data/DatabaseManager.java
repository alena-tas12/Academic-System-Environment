package sms.system_services.data;

import java.sql.*;
import java.nio.file.*;
import java.util.*;

/**
 * Embedded persistent database boundary for the whole application.
 * The web UI and domain runtime use this same database instance.
 */
public final class DatabaseManager {
    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            Path dataDir = Paths.get("data");
            Files.createDirectories(dataDir);
            connection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + dataDir.resolve("academic").toAbsolutePath(),
                    "sa", "");
            initializeSchema();
            seed();
            System.out.println("[Database] Embedded HSQLDB connected.");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize database", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private void initializeSchema() throws SQLException {
        create("CREATE TABLE students (" +
                "id VARCHAR(40) PRIMARY KEY, name VARCHAR(120), email VARCHAR(180), phone VARCHAR(40)," +
                "department VARCHAR(120), semester INTEGER, type VARCHAR(30), status VARCHAR(30)," +
                "admission_date VARCHAR(30), attendance DOUBLE, created_at TIMESTAMP, updated_at TIMESTAMP)");
        create("CREATE TABLE subjects (code VARCHAR(30) PRIMARY KEY, name VARCHAR(120), credits INTEGER)");
        create("CREATE TABLE classes (id INTEGER IDENTITY, name VARCHAR(120), department VARCHAR(120), semester INTEGER, room VARCHAR(60), capacity INTEGER)");
        create("CREATE TABLE marks (id INTEGER IDENTITY, student_id VARCHAR(40), subject_code VARCHAR(30), mark DOUBLE, updated_at TIMESTAMP)");
        create("CREATE TABLE attendance (id INTEGER IDENTITY, student_id VARCHAR(40), subject_code VARCHAR(30), present BOOLEAN, recorded_at TIMESTAMP)");
        create("CREATE TABLE audit_logs (id INTEGER IDENTITY, timestamp TIMESTAMP, actor VARCHAR(80), action VARCHAR(120), details VARCHAR(1000))");
        create("CREATE TABLE system_events (id INTEGER IDENTITY, timestamp TIMESTAMP, type VARCHAR(100), student_id VARCHAR(40), payload VARCHAR(2000))");
    }

    private void create(String sql) {
        try (Statement s = connection.createStatement()) { s.executeUpdate(sql); }
        catch (SQLException e) {
            // 42504/42504 varies by HSQL version; an existing table is harmless.
            if (!e.getMessage().toLowerCase().contains("already exists")) throw new IllegalStateException(e);
        }
    }

    private boolean empty(String table) throws SQLException {
        try (Statement s=connection.createStatement(); ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)) {
            r.next(); return r.getInt(1)==0;
        }
    }

    private void seed() throws SQLException {
        if (empty("subjects")) {
            addSubject("MATH201","Engineering Mathematics",4);
            addSubject("CS201","Object Oriented Programming",4);
            addSubject("CS203","Computer Networks",3);
            addSubject("CS205","Operating Systems",3);
            addSubject("DS201","Data Science Fundamentals",3);
        }
        if (empty("classes")) {
            addClass("CE-5A","Computer Engineering",5,"B-204",60);
            addClass("CE-5B","Computer Engineering",5,"B-205",60);
            addClass("DS-5A","Data Science",5,"C-103",50);
        }
        if (empty("students")) {
            addStudent("24CE1034","Vian Barnes","vian@example.edu","+91 90000 00000","Computer Engineering",5,"UNDERGRADUATE","ACTIVE");
            addStudent("24CE1035","Rahul Kumar","rahul@example.edu","+91 90000 00001","Computer Engineering",5,"UNDERGRADUATE","ACTIVE");
            addStudent("24CE1036","Arjun Singh","arjun@example.edu","+91 90000 00002","Data Science",5,"UNDERGRADUATE","ACTIVE");
            addStudent("24CE1037","Ananya Rao","ananya@example.edu","+91 90000 00003","Cybersecurity",5,"UNDERGRADUATE","ACTIVE");
            addStudent("24CE1038","Meera Nair","meera@example.edu","+91 90000 00004","Computer Engineering",5,"UNDERGRADUATE","ACTIVE");
            addMark("24CE1034","MATH201",82);
            addMark("24CE1034","CS201",91);
            addMark("24CE1034","CS203",76);
            addMark("24CE1034","CS205",88);
            addMark("24CE1035","MATH201",73);
            addMark("24CE1035","CS201",79);
            addMark("24CE1036","MATH201",48);
            addMark("24CE1036","CS201",55);
            addMark("24CE1037","MATH201",92);
        }
    }

    public synchronized void addStudent(String id,String name,String email,String phone,String dept,int semester,String type,String status) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO students VALUES(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")) {
            p.setString(1,id);p.setString(2,name);p.setString(3,email);p.setString(4,phone);p.setString(5,dept);p.setInt(6,semester);
            p.setString(7,type);p.setString(8,status);p.setString(9,java.time.LocalDate.now().toString());p.setDouble(10,85.0);p.executeUpdate();
        }
    }

    public synchronized void updateStudent(String id,String name,String email,String phone,String dept,int semester,String status) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("UPDATE students SET name=?,email=?,phone=?,department=?,semester=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            p.setString(1,name);p.setString(2,email);p.setString(3,phone);p.setString(4,dept);p.setInt(5,semester);p.setString(6,status);p.setString(7,id);p.executeUpdate();
        }
    }

    public synchronized void deleteStudent(String id) throws SQLException {
        for(String q:new String[]{"DELETE FROM marks WHERE student_id=?","DELETE FROM attendance WHERE student_id=?","DELETE FROM students WHERE id=?"}) {
            try(PreparedStatement p=connection.prepareStatement(q)){p.setString(1,id);p.executeUpdate();}
        }
    }

    public synchronized void addMark(String sid,String subject,double mark) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO marks(student_id,subject_code,mark,updated_at) VALUES(?,?,?,CURRENT_TIMESTAMP)")) {
            p.setString(1,sid);p.setString(2,subject);p.setDouble(3,mark);p.executeUpdate();
        }
    }
    public synchronized void upsertMark(String sid,String subject,double mark) throws SQLException {
        try(PreparedStatement d=connection.prepareStatement("DELETE FROM marks WHERE student_id=? AND subject_code=?")) {d.setString(1,sid);d.setString(2,subject);d.executeUpdate();}
        addMark(sid,subject,mark);
    }
    public synchronized void recordAttendance(String sid,String subject,boolean present) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO attendance(student_id,subject_code,present,recorded_at) VALUES(?,?,?,CURRENT_TIMESTAMP)")) {
            p.setString(1,sid);p.setString(2,subject);p.setBoolean(3,present);p.executeUpdate();
        }
        try(PreparedStatement p=connection.prepareStatement("UPDATE students SET attendance=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            p.setDouble(1,attendance(sid));p.setString(2,sid);p.executeUpdate();
        }
    }
    public synchronized double attendance(String sid) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("SELECT COUNT(*),SUM(CASE WHEN present THEN 1 ELSE 0 END) FROM attendance WHERE student_id=?")) {
            p.setString(1,sid);try(ResultSet r=p.executeQuery()){r.next();int total=r.getInt(1);int present=r.getInt(2);return total==0?85.0:(present*100.0/total);}
        }
    }

    public synchronized void addSubject(String code,String name,int credits) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO subjects VALUES(?,?,?)")) {p.setString(1,code);p.setString(2,name);p.setInt(3,credits);p.executeUpdate();}
    }
    public synchronized void addClass(String name,String dept,int sem,String room,int cap) throws SQLException {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO classes(name,department,semester,room,capacity) VALUES(?,?,?,?,?)")) {p.setString(1,name);p.setString(2,dept);p.setInt(3,sem);p.setString(4,room);p.setInt(5,cap);p.executeUpdate();}
    }

    public synchronized List<Map<String,Object>> query(String sql,Object... args) throws SQLException {
        List<Map<String,Object>> out=new ArrayList<>();
        try(PreparedStatement p=connection.prepareStatement(sql)){
            for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
            try(ResultSet r=p.executeQuery()){ResultSetMetaData m=r.getMetaData();while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int c=1;c<=m.getColumnCount();c++)row.put(m.getColumnLabel(c).toLowerCase(),r.getObject(c));out.add(row);}}
        } return out;
    }
    public synchronized void executeUpdate(String sql,Object... args) {
        try(PreparedStatement p=connection.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();}
        catch(SQLException e){System.err.println("[Database] "+e.getMessage());}
    }
    public synchronized void checkpoint(){ try(Statement s=connection.createStatement()){s.executeUpdate("CHECKPOINT");}catch(Exception ignored){} }
    public synchronized void close(){try(Statement s=connection.createStatement()){s.executeUpdate("SHUTDOWN");}catch(Exception ignored){}}
}
