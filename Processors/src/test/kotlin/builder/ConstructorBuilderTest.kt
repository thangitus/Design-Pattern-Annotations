package builder

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.*
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Method

internal class ConstructorBuilderTest {

    @Test
    fun annotatedWithField() {
        val result = compile(
                """
            package builder;
            
            class User {
                String name;
                
                @Builder
                private String email;
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("Only class can be annotated with @Builder")
    }

    @Test
    fun annotatedWithMethod() {
        val result = compile(
                """
            package builder;
            
            class User {
                String name;
                
                private String email;            

                @Builder
                public void doSomething(){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("Only class can be annotated with @Builder")
    }

    @Test
    fun constructorNotEnoughParams() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name;
                
                private String email;
                            
                User(String name){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("The annotated class must have constructor with all params")
    }

    @Test
    fun constructorIsPrivate() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name;
                
                private String email;
                            
                private User(String name, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("Constructor with all params must be public or protected")
    }

    @Test
    fun constructorIsAmbiguousName() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name;
                
                private String email;
                            
                User(String abc, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("Constructor is ambiguous, it should be User(String name, String email)")
    }

    @Test
    fun constructorIsAmbiguousType() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name;
                
                private String email;
                            
                User(int name, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)
        assertThat(result.messages).contains("Constructor is ambiguous, it should be User(String name, String email)")
    }

    @Test
    fun numberMethod() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name1;
                String name2;
                String name3;
                private String email;
                            
                User(String name1, String name2 ,String name3, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(OK)
        val builderClass = loadUserBuilderClass(result)
        val declaredMethods = getDeclaredMethodsMap(builderClass)
        assertThat(declaredMethods.size).isEqualTo(5)
    }

    @Test
    fun numberField() {
        val result = compile(
                """
            package builder;
            
            @Builder
            class User {
                String name1;
                String name2;
                String name3;
                private String email;
                            
                User(String name1, String name2 ,String name3, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(OK)
        val builderClass = loadUserBuilderClass(result)
        val declaredFields = getDeclaredFieldsMap(builderClass)
        assertThat(declaredFields.size).isEqualTo(4)
    }

    @Test
    fun customNameMethodBuild() {
        val result = compile(
                """
            package builder;
            
            @Builder(buildMethodName = "abc")
            class User {
                String name;
                String email;
                            
                User(String name, String email){};
            }

        """.trimIndent())
        assertThat(result.exitCode).isEqualTo(OK)
        val builderClass = loadUserBuilderClass(result)
        val declaredMethods = getDeclaredMethodsMap(builderClass)
        assertThat(declaredMethods.size).isEqualTo(3)
        assertThat(declaredMethods).containsKey("abc")
    }

    private fun loadBuilderClass(result: KotlinCompilation.Result, className: String): Class<*> =
            result.classLoader.loadClass(className)

    private fun loadUserBuilderClass(result: KotlinCompilation.Result): Class<*> =
            loadBuilderClass(result, "builder.UserBuilder")

    private fun getDeclaredMethodsMap(builderClass: Class<*>): Map<String, Method> =
            builderClass.declaredMethods.map { it.name to it }.toMap<String, Method>()

    private fun getDeclaredFieldsMap(builderClass: Class<*>): Map<String, Field> =
            builderClass.declaredFields.map { it.name to it }.toMap<String, Field>()

    private fun compile(@Language("java") source: String): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = listOf(
                    SourceFile.java("User.java", source))
            messageOutputStream = System.out
            annotationProcessors = listOf(BuilderProcessor())
            verbose = false
            inheritClassPath = true
        }.compile()
    }
}