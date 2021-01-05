package builder;

@Builder
class User {
   String name;
   String email;
   int old;

   User(String name, String email, int old) {
      this.name = name;
      this.email = email;
      this.old = old;
   };
}