package university;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class Server {

    public interface UniversityService extends Remote {

        void addTeacher(int id, String name, String department) throws RemoteException, SQLException;
        String listTeachers() throws RemoteException, SQLException;

        void addStudent(int id, String name, String department,
                          char section, int year) throws RemoteException, SQLException;
        String listStudents() throws RemoteException, SQLException;
        boolean login(String role, int id, String name) throws RemoteException, SQLException;

        int  sendMessage(ChatMessage msg) throws RemoteException, SQLException;
        List<ChatMessage> getMessages(int afterId) throws RemoteException, SQLException;
    }

    public static class UniversityServiceImpl
            extends UnicastRemoteObject implements UniversityService {
        protected UniversityServiceImpl() throws RemoteException { super(); }
        private void ensureSchema(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS TEACHER (" +
                                "  id         INT PRIMARY KEY," +
                                "  name       VARCHAR(100)," +
                                "  department VARCHAR(100)" +
                                ")");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS STUDENT (" +
                                "  id         INT PRIMARY KEY," +
                                "  name       VARCHAR(100)," +
                                "  department VARCHAR(100)," +
                                "  section    CHAR(1)," +
                                "  year       INT" +
                                ")");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS CHAT_MESSAGE (" +
                                "  id          INT AUTO_INCREMENT PRIMARY KEY," +
                                "  sender_role VARCHAR(10)," +
                                "  sender_name VARCHAR(100)," +
                                "  msg_type    VARCHAR(4)," +
                                "  content     TEXT," +
                                "  file_data   LONGBLOB," +
                                "  ts          BIGINT" +
                                ")");
            }
        }
        @Override
        public synchronized void addTeacher(int id, String name, String department) throws RemoteException, SQLException {
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO TEACHER (id, name, department) VALUES (?,?,?) " +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name), department=VALUES(department)");
                ps.setInt(1, id); ps.setString(2, name); ps.setString(3, department);
                ps.executeUpdate();
            }
            try {
                Path textFile = Path.of("teachers_backup.txt");
                String textLine = String.format("%s,%d,\"%s\",\"%s\"\n",
                        LocalDateTime.now(), id, name, department);
                Files.writeString(textFile, textLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Warning: Teacher saved to DB, but Text backup failed: " + e.getMessage());
            }
            try {
                TeacherData wrapper = new TeacherData(id, name, department);
                String filename = String.format("teacher_%d.ser", id);
                try (FileOutputStream fileOut = new FileOutputStream(filename);
                     ObjectOutputStream objOut = new ObjectOutputStream(fileOut)) {
                    objOut.writeObject(wrapper);
                }
            } catch (IOException e) {
                System.err.println("Warning: Teacher saved to DB, but Serialized backup failed: " + e.getMessage());
            }
        }
        static class TeacherData implements Serializable {
            private static final long serialVersionUID = 1L;
            public int id;
            public String name;
            public String department;
            public LocalDateTime backupTime;
            public TeacherData(int id, String name, String department) {
                this.id = id;
                this.name = name;
                this.department = department;
                this.backupTime = LocalDateTime.now();
            }
        }
        @Override
        public String listTeachers() throws RemoteException, SQLException {
            StringBuilder sb = new StringBuilder();
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM TEACHER ORDER BY id");
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                            .append(" | Name: ").append(rs.getString("name"))
                            .append(" | Dept: ").append(rs.getString("department"))
                            .append("\n");
                }
            }
            return sb.length() == 0 ? "No teacher records found." : sb.toString();
        }

        @Override
        public synchronized void addStudent(int id, String name, String department, char section, int year) throws RemoteException, SQLException {
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO STUDENT (id, name, department, section, year) VALUES (?,?,?,?,?) " +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name), department=VALUES(department)," +
                                "  section=VALUES(section), year=VALUES(year)");
                ps.setInt(1, id); ps.setString(2, name); ps.setString(3, department);
                ps.setString(4, String.valueOf(section)); ps.setInt(5, year);
                ps.executeUpdate();
            }
            try {
                Path csvFile = Path.of("students_backup.txt");
                String csvLine = String.format("%s,%d,\"%s\",\"%s\",%c,%d\n",
                        LocalDateTime.now(), id, name, department, section, year);
                Files.writeString(csvFile, csvLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Warning: Student saved to DB, but Text backup failed: " + e.getMessage());
            }
            try {
                StudentData wrapper = new StudentData(id, name, department, section, year);
                String filename = String.format("student_%d.ser", id);
                try (FileOutputStream fileOut = new FileOutputStream(filename);
                     ObjectOutputStream objOut = new ObjectOutputStream(fileOut)) {

                    objOut.writeObject(wrapper);
                }
            } catch (IOException e) {
                System.err.println("Warning: Student saved to DB, but Serialized backup failed: " + e.getMessage());
            }
        }
        static class StudentData implements Serializable {
            private static final long serialVersionUID = 1L;
            public int id;
            public String name;
            public String department;
            public char section;
            public int year;
            public LocalDateTime backupTime;
            public StudentData(int id, String name, String department, char section, int year) {
                this.id = id;
                this.name = name;
                this.department = department;
                this.section = section;
                this.year = year;
                this.backupTime = LocalDateTime.now();
            }
        }

        @Override
        public String listStudents() throws RemoteException, SQLException {
            StringBuilder sb = new StringBuilder();
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM STUDENT ORDER BY id");
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                            .append(" | Name: ").append(rs.getString("name"))
                            .append(" | Dept: ").append(rs.getString("department"))
                            .append(" | Sec: ").append(rs.getString("section"))
                            .append(" | Year: ").append(rs.getInt("year"))
                            .append("\n");
                }
            }
            return sb.length() == 0 ? "No student records found." : sb.toString();
        }

        @Override
        public boolean login(String role, int id, String name)
                throws RemoteException, SQLException {
            String table = "TEACHER".equalsIgnoreCase(role) ? "TEACHER" : "STUDENT";
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM " + table + " WHERE id=? AND name=?");
                ps.setInt(1, id); ps.setString(2, name);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }

        @Override
        public synchronized int sendMessage(ChatMessage msg)
                throws RemoteException, SQLException {
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CHAT_MESSAGE (sender_role, sender_name, msg_type, content, file_data, ts)" +
                                " VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, msg.senderRole);
                ps.setString(2, msg.senderName);
                ps.setString(3, msg.type.name());
                ps.setString(4, msg.content);
                if (msg.fileData != null) ps.setBytes(5, msg.fileData);
                else                     ps.setNull(5, Types.BLOB);
                ps.setLong(6, msg.timestamp);
                ps.executeUpdate();
                ResultSet gk = ps.getGeneratedKeys();
                int newId = gk.next() ? gk.getInt(1) : -1;
                // Notify relay clients
                socketRelay.broadcast(msg.toString());
                return newId;
            }
        }

        @Override
        public List<ChatMessage> getMessages(int afterId)
                throws RemoteException, SQLException {
            List<ChatMessage> list = new ArrayList<>();
            try (Connection conn = Database.getConnection()) {
                ensureSchema(conn);
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM CHAT_MESSAGE WHERE id > ? ORDER BY id");
                ps.setInt(1, afterId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ChatMessage.Type t = ChatMessage.Type.valueOf(rs.getString("msg_type"));
                    list.add(new ChatMessage(
                            rs.getInt("id"),
                            rs.getString("sender_role"),
                            rs.getString("sender_name"),
                            t,
                            rs.getString("content"),
                            rs.getBytes("file_data"),
                            rs.getLong("ts")
                    ));
                }
            }
            return list;
        }
    }

    static SocketRelay socketRelay = new SocketRelay(5050);

    public static class SocketRelay {
        private final int port;
        private final Set<PrintWriter> writers = ConcurrentHashMap.newKeySet();

        public SocketRelay(int port) { this.port = port; }

        public void start() {
            Thread t = new Thread(() -> {
                try (ServerSocket ss = new ServerSocket(port)) {
                    System.out.println("[SocketRelay] Listening on port " + port);
                    while (true) {
                        Socket client = ss.accept();
                        PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                        writers.add(pw);
                        // Remove writer when client disconnects
                        new Thread(() -> {
                            try { client.getInputStream().read(); } catch (IOException ignored) {}
                            writers.remove(pw);
                        }).start();
                    }
                } catch (IOException e) {
                    System.err.println("[SocketRelay] Error: " + e.getMessage());
                }
            });
            t.setDaemon(true);
            t.start();
        }

        public void broadcast(String line) {
            for (PrintWriter pw : writers) pw.println("NEW_MSG:" + line);
        }
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            UniversityServiceImpl impl = new UniversityServiceImpl();
            Naming.rebind("rmi://localhost/UniversityService", impl);
            System.out.println("[RMI] UniversityService bound on port 1099.");

            socketRelay.start();
            System.out.println("[Server] Ready. Waiting for connections...");
        } catch (Exception e) {
            System.err.println("[Server] Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
