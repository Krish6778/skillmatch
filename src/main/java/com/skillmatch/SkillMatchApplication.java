package com.skillmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class SkillMatchApplication {

    // Temp database
    private final List<Student> students = new ArrayList<>();
    private final List<Team> teams = new ArrayList<>();
    private final List<JoinRequest> requests = new ArrayList<>();

    private final AtomicLong nextTeamId = new AtomicLong(1);
    private final AtomicLong nextRequestId = new AtomicLong(1);

    public SkillMatchApplication() {
        students.add(new Student(1L, "Krish Rana", "krish@example.com", "demo123"));
        students.add(new Student(2L, "Aditi Sharma", "aditi@example.com", "demo123"));
        students.add(new Student(3L, "Arjun Mehta", "arjun@example.com", "demo123"));

        //sample team
        Team sample = new Team(nextTeamId.getAndIncrement(),
                "Campus Connect", "A platform for students to discover campus events.",
                4, 2L);
        sample.members.add(2L);
        teams.add(sample);
    }

    public static void main(String[] args) {
        SpringApplication.run(SkillMatchApplication.class, args);
    }

    //data

    public static class Student {
        public Long id;
        public String name;
        public String email;
        public String password;

        public Student() {}

        public Student(Long id, String name, String email, String password) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    public static class Team {
        public Long id;
        public String name;
        public String description;
        public int maxMembers;
        public Long leaderId;
        public List<Long> members = new ArrayList<>();

        public Team() {}

        public Team(Long id, String name, String description,
                    int maxMembers, Long leaderId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.maxMembers = maxMembers;
            this.leaderId = leaderId;
            this.members.add(leaderId);
        }
    }

    public static class JoinRequest {
        public Long id;
        public Long teamId;
        public Long studentId;
        public String status = "PENDING";

        public JoinRequest() {}

        public JoinRequest(Long id, Long teamId, Long studentId) {
            this.id = id;
            this.teamId = teamId;
            this.studentId = studentId;
        }
    }

    //fcfs concept
    public static class SchedulingJob {
        public String name;
        public int arrivalTime;
        public int burstTime;
        public int startTime;
        public int completionTime;
        public int waitingTime;
        public int turnaroundTime;

        public SchedulingJob() {}
    }

    public static class LoginInput {
        public String email;
        public String password;
    }

    public static class TeamInput {
        public String name;
        public String description;
        public int maxMembers;
        public Long leaderId;
    }

    public static class RequestInput {
        public Long studentId;
    }

    //login

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody LoginInput input) {
        Student student = students.stream()
                .filter(s -> s.email.equalsIgnoreCase(input.email == null ? "" : input.email))
                .filter(s -> s.password.equals(input.password == null ? "" : input.password))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", student.id);
        result.put("name", student.name);
        result.put("email", student.email);
        return result;
    }

    // dashboard

    @GetMapping("/api/teams")
    public List<Map<String, Object>> getTeams() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Team team : teams) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", team.id);
            item.put("name", team.name);
            item.put("description", team.description);
            item.put("maxMembers", team.maxMembers);
            item.put("leaderId", team.leaderId);
            item.put("leaderName", findStudent(team.leaderId).name);
            item.put("members", team.members);
            item.put("memberCount", team.members.size());
            result.add(item);
        }

        return result;
    }

    @GetMapping("/api/students")
    public List<Map<String, Object>> getStudents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", student.id);
            item.put("name", student.name);
            item.put("email", student.email);
            result.add(item);
        }
        return result;
    }

    @GetMapping("/api/dashboard/{studentId}")
    public Map<String, Object> dashboard(@PathVariable Long studentId) {
        findStudent(studentId);

        long myTeams = teams.stream()
                .filter(t -> t.leaderId.equals(studentId) || t.members.contains(studentId))
                .count();

        long pendingRequests = requests.stream()
                .filter(r -> r.status.equals("PENDING"))
                .filter(r -> {
                    Team team = findTeam(r.teamId);
                    return team.leaderId.equals(studentId);
                }).count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teamCount", myTeams);
        data.put("availableTeams", teams.size());
        data.put("pendingRequests", pendingRequests);
        return data;
    }

    // create team
    @PostMapping("/api/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTeam(@RequestBody TeamInput input) {
        Student leader = findStudent(input.leaderId);

        if (input.name == null || input.name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a team name");
        }

        if (input.maxMembers < 2 || input.maxMembers > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Team size must be between 2 and 10");
        }

        Team team = new Team(nextTeamId.getAndIncrement(),
                input.name.trim(),
                input.description == null ? "" : input.description.trim(),
                input.maxMembers, leader.id);
        teams.add(team);

        return teamSummary(team);
    }

    // Join requests 

    @PostMapping("/api/teams/{teamId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequest sendJoinRequest(@PathVariable Long teamId,
                                       @RequestBody RequestInput input) {
        Team team = findTeam(teamId);
        Student student = findStudent(input.studentId);

        if (team.leaderId.equals(student.id) || team.members.contains(student.id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You are already part of this team");
        }

        if (team.members.size() >= team.maxMembers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This team is already full");
        }

        boolean alreadyRequested = requests.stream()
                .anyMatch(r -> r.teamId.equals(teamId)
                        && r.studentId.equals(student.id)
                        && r.status.equals("PENDING"));

        if (alreadyRequested) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You already have a pending request for this team");
        }

        JoinRequest request = new JoinRequest(
                nextRequestId.getAndIncrement(), teamId, student.id);
        requests.add(request);
        return request;
    }

    @GetMapping("/api/teams/{teamId}/requests")
    public List<Map<String, Object>> getTeamRequests(@PathVariable Long teamId,
                                                     @RequestParam Long leaderId) {
        Team team = findTeam(teamId);
        checkLeader(team, leaderId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (JoinRequest request : requests) {
            if (request.teamId.equals(teamId)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", request.id);
                item.put("teamId", request.teamId);
                item.put("studentId", request.studentId);
                item.put("studentName", findStudent(request.studentId).name);
                item.put("status", request.status);
                result.add(item);
            }
        }
        return result;
    }

    @PatchMapping("/api/requests/{requestId}")
    public synchronized JoinRequest updateRequest(@PathVariable Long requestId,
                                     @RequestParam Long leaderId,
                                     @RequestParam String status) {
        JoinRequest request = requests.stream()
                .filter(r -> r.id.equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Request not found"));

        Team team = findTeam(request.teamId);
        checkLeader(team, leaderId);

        String newStatus = status.toUpperCase(Locale.ROOT);
        if (!newStatus.equals("ACCEPTED") && !newStatus.equals("REJECTED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be ACCEPTED or REJECTED");
        }

        if (!request.status.equals("PENDING")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This request has already been handled");
        }

        if (newStatus.equals("ACCEPTED")) {
            if (team.members.size() >= team.maxMembers) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The team is already full");
            }
            team.members.add(request.studentId);
        }

        request.status = newStatus;
        return request;
    }

    // FCFS CPU scheduling

    @PostMapping("/api/os/fcfs")
    public Map<String, Object> runFcfs(@RequestBody List<SchedulingJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please provide at least one process");
        }

        for (SchedulingJob job : jobs) {
            if (job.name == null || job.name.isBlank()
                    || job.arrivalTime < 0 || job.burstTime <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each process needs a name, arrival time >= 0 and burst time > 0");
            }
        }

        // FCFS runs jobs in the order they arrive.
        List<SchedulingJob> queue = new ArrayList<>(jobs);
        queue.sort(Comparator.comparingInt(job -> job.arrivalTime));

        int currentTime = 0;
        double totalWaiting = 0;
        double totalTurnaround = 0;

        for (SchedulingJob job : queue) {
            job.startTime = Math.max(currentTime, job.arrivalTime);
            job.completionTime = job.startTime + job.burstTime;
            job.waitingTime = job.startTime - job.arrivalTime;
            job.turnaroundTime = job.completionTime - job.arrivalTime;

            currentTime = job.completionTime;
            totalWaiting += job.waitingTime;
            totalTurnaround += job.turnaroundTime;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "First Come, First Served (FCFS)");
        result.put("processes", queue);
        result.put("averageWaitingTime",
                Math.round(totalWaiting / queue.size() * 100.0) / 100.0);
        result.put("averageTurnaroundTime",
                Math.round(totalTurnaround / queue.size() * 100.0) / 100.0);
        result.put("explanation",
                "Processes are run in arrival order. Waiting Time = Start Time - Arrival Time; Turnaround Time = Completion Time - Arrival Time.");
        return result;
    }

    // Helper methods
    private Student findStudent(Long id) {
        return students.stream()
                .filter(s -> s.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found"));
    }

    private Team findTeam(Long id) {
        return teams.stream()
                .filter(t -> t.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Team not found"));
    }

    private void checkLeader(Team team, Long leaderId) {
        if (!team.leaderId.equals(leaderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the team leader can manage requests");
        }
    }

    private Map<String, Object> teamSummary(Team team) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", team.id);
        item.put("name", team.name);
        item.put("description", team.description);
        item.put("maxMembers", team.maxMembers);
        item.put("leaderId", team.leaderId);
        item.put("leaderName", findStudent(team.leaderId).name);
        item.put("members", team.members);
        item.put("memberCount", team.members.size());
        return item;
    }
}
