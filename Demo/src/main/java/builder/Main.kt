package builder

class Main {
    var generateTest: List<String>? = null

    fun main(args: Array<String?>?) {
        val user = UserBuilder().setEmail("Abc")
            .setName("Def")
            .setOld(10)
            .build()
        bindGenerationValue(this)

        generateTest?.forEach {
            print(it.length)
        }
    }
}