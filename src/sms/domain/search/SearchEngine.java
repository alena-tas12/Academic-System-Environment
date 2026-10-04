package sms.domain.search;
import java.util.List;
public class SearchEngine {
    public void executeAdvancedSearch(SearchCriteria criteria) {
        System.out.println("[SearchEngine] Executing multi-criteria query...");
        System.out.println("  -> Department = " + criteria.department);
        System.out.println("  -> Min Attendance = " + criteria.minAttendance + "%");
        System.out.println("[SearchEngine] Found 17 matching students.");
    }
}
