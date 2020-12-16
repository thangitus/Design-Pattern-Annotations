package builder;


@Builder
class User {
    String name1;
    String name2;
    String name3;
    private String email;

    User(String name1, String name2 ,String name3, String email){};
}