package builder;

class Main {
   public static void main(String[] args) {
      User user = new UserBuilder().setEmail("Abc")
                                   .setName("Def")
                                   .setOld(10)
                                   .build();
   }
}
