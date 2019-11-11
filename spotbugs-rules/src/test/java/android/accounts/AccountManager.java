package android.accounts;

public class AccountManager {

    public static AccountManager get() {
        return new AccountManager();
    }

    public void getAccounts() {
        selfCall();
    }

    private void selfCall() {

    }
}
