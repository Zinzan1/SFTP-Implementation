package main;

public class Profile {
    private final String UserID;
    private final String Password;
    private final String Account;

    public Profile(String acc, String pw, String id) {
        UserID = id;
        Password = pw;
        Account = acc;
    }

    public String getUserID() {
        return UserID;
    }

    public String getPassword() {
        return Password;
    }

    public String getAccount() {
        return Account;
    }
}
