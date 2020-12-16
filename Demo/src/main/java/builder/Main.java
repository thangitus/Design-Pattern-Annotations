package builder;

class Main {
    public static void main(String[] args) {
        User user = new UserBuilder().setName("gg").build();
    }
}
