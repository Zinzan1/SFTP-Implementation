package main;

public class Authentication {
    private final long UserID;
    private final String Password;
    private final String Account;

    public Authentication(String acc, String pw, long id) {
        UserID = id;
        Password = pw;
        Account = acc;
    }

    public long getUserID() {
        return UserID;
    }

    public String getPassword() {
        return Password;
    }

    public String getAccount() {
        return Account;
    }
}
