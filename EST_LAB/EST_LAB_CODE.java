public class EST_LAB_CODE {
    public static void main(String[] args) {
        UserDetails user = new UserDetails("Shiva Gupta", 123456789, 1000);
        Transactions transactions = new Transactions(user);

        System.out.println("Initial Balance: " + transactions.deposit(500));
        System.out.println("Balance after withdrawal: " + transactions.withdraw(300));

        UpdateDetails updateDetails = new UpdateDetails(user);
        updateDetails.setName("Shiva");
        user.setEmail("shiva@example.com");

        System.out.println("Updated Username: " + user.getUserName());
        System.out.println("Updated Email: " + user.getEmail());
    }
}

class UserDetails {
    private String userName;
    private long accountNo;
    private int balance;
    private String email;

    public UserDetails(String userName, long accountNo, int balance) {
        this.userName = userName;
        this.accountNo = accountNo;
        this.balance = balance;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getAccountNo() {
        return accountNo;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

class Transactions {
    private UserDetails userDetails;

    public Transactions(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public int withdraw(int amount) {
        if (userDetails.getBalance() - amount < 0) {
            System.out.println("Cannot withdraw as balance is low");
            return -1;
        } else {
            userDetails.setBalance(userDetails.getBalance() - amount);
            return userDetails.getBalance();
        }
    }

    public int deposit(int amount) {
        userDetails.setBalance(userDetails.getBalance() + amount);
        return userDetails.getBalance();
    }
}

class UpdateDetails {
    private UserDetails userDetails;

    public UpdateDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public void setName(String name) {
        userDetails.setUserName(name);
    }

    public void setEmail(String email) {
        userDetails.setEmail(email);
    }
}
