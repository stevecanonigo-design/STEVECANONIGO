CafeClock - Computer Cafe Time System (Authentication + Admin Approval)
Run from this folder so students.db is found:
  mvn compile exec:java
IntelliJ: open pom.xml as project, run Main.java (working dir = project root).

Roles:    CUSTOMER, CAFE_STAFF, ADMIN
Statuses: PENDING (new registration), ACTIVE (can log in), INACTIVE (disabled)

There is NO default admin. On the first run (no ADMIN yet) the program asks you to create it.
Public registration only offers CUSTOMER and CAFE_STAFF; only an Admin can create another Admin.
Only ACTIVE accounts can log in. Admin Menu: review pending accounts - approve or reject (3), create (4), delete (5).
