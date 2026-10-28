package sg.edu.ntu.m3_simple_crm.service;

public class DemoService {

    public int calculateAge(int yearOfBirth, int currentYear) {
        return currentYear - yearOfBirth;
    }

    public String formatFullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    public String getInitials(String first, String last) {
        if (first == null || first.isEmpty()) {
            return "";
        }
        if (last == null || last.isEmpty()) {
            return "";
        }

        String firstInitial = first.substring(0, 1).toUpperCase();
        String lastInitial = last.substring(0, 1).toUpperCase();

        return firstInitial + lastInitial;
    }

}
