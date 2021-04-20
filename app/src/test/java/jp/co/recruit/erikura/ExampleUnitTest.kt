package jp.co.recruit.erikura

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.apache.commons.lang.builder.ToStringBuilder
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.regex.Pattern

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun parseTimeTest() {
        val timeInMillisStr = "1612683468463"
        val timeInMillis = timeInMillisStr.toLong()
        print(Date(timeInMillis))
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    open class NotificationData(val open: String)

    @Test
    fun whenTest() {
        data class DummyReport(var reported: Boolean, val expired: Boolean, val accepted: Boolean, val rejected: Boolean) {
            val isReportCreatable: Boolean get() {
                return when {
                    reported -> false
                    expired  -> false
                    else     -> true
                }
            }
            val isReportEditable: Boolean get() {
                return when {
                    !reported -> false
                    accepted  -> false
                    rejected  -> true
                    else      -> true
                }
            }
            val isReportEditable2: Boolean get() =
                when {
                    !reported -> false
                    accepted  -> false
                    rejected  -> true
                    else      -> true
                }
        }

        DummyReport(reported = false, expired = false, accepted = false, rejected = false).let {
            assertTrue(it.isReportCreatable)
            assertFalse(it.isReportEditable)
        }
        DummyReport(reported = false, expired = true, accepted = false, rejected = false).let {
            assertFalse(it.isReportCreatable)
            assertFalse(it.isReportEditable)
        }

        DummyReport(reported = true, expired = false, accepted = false, rejected = false).let {
            assertFalse(it.isReportCreatable)
            assertTrue(it.isReportEditable)
        }
        DummyReport(reported = true, expired = false, accepted = true, rejected = false).let {
            assertFalse(it.isReportCreatable)
            assertFalse(it.isReportEditable)
        }
        DummyReport(reported = true, expired = false, accepted = false, rejected = true).let {
            assertFalse(it.isReportCreatable)
            assertTrue(it.isReportEditable)
        }

        DummyReport(reported = true, expired = true, accepted = false, rejected = false).let {
            assertFalse(it.isReportCreatable)
            assertTrue(it.isReportEditable)
        }
        DummyReport(reported = true, expired = true, accepted = true, rejected = false).let {
            assertFalse(it.isReportCreatable)
            assertFalse(it.isReportEditable)
        }
        DummyReport(reported = true, expired = true, accepted = false, rejected = true).let {
            assertFalse(it.isReportCreatable)
            assertTrue(it.isReportEditable)
        }

        DummyReport(reported = false, expired = false, accepted = false, rejected = true).let {
            assertFalse(it.isReportEditable2)

            it.reported = true

            assertTrue(it.isReportEditable2)
        }
    }

    @Test
    fun emailPatternTest() {
        val emailPattern = """\A[\w._%+-|]+@[\w0-9.-]+\.[A-Za-z]{2,}\z""".toRegex()

        assertTrue(emailPattern.matches("test@example.com"))
        assertTrue(emailPattern.matches("test.hoge@example.com"))
        assertTrue(emailPattern.matches("test-hoge@example.com"))
        assertTrue(emailPattern.matches("test%hoge@example.com"))
        assertTrue(emailPattern.matches("test+hoge@example.com"))
        assertTrue(emailPattern.matches("test_hoge@example.com"))
        assertTrue(emailPattern.matches("test|hoge@example.com"))
        assertTrue(emailPattern.matches("test@example.test-sample.com"))
        assertFalse(emailPattern.matches("test!hoge@example.com"))
    }

    @Test
    fun gsonParse() {
        val jsonString = "{\"open\": \"/jobs/345\"}"

        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create()

        val result = gson.fromJson(jsonString, NotificationData::class.java)
        assertEquals(result.open, "/jobs/345")
        val string = ToStringBuilder.reflectionToString(result)

        assertEquals(string, "dummy")

/*
        // JSONからStringへの変換
        String str = gson.fromJson("\"hello\"", String.class);
        System.out.println("String: " + str);

        // JSONからJavaオブジェクトへの変換
        User user = gson.fromJson("{\"email\":\"bob@jmail.com\",\"fullname\":\"Bob\"}", User.class);
        System.out.println("User: " + user.email + " / " + user.fullname);

        // JSONから配列への変換
        int[] array = gson.fromJson("[1, 2, 3]", int[].class);
        System.out.println("int[]: " + array[0] + ",　" + array[1] + ",　" + array[2]);

        // JSONからListへの変換
        List list = gson.fromJson("[\"hello\", \"hellohello\",\"hellohellohello\"]", List.class);
        System.out.println("List: " + list.get(0) + ",　" + list.get(1) + ",　" + list.get(2));

        // JSONからフィールドにListを含むJavaオブジェクトへの変換
        String jsonStr = "{\"title\":\"投稿タイトル\",\"content\":\"本文本文本文\","
                + "\"author\":{\"email\":\"bob@jmail.com\",\"fullname\":\"Bob\"},"
                + "\"comments\":[{\"author\":\"Tom\",\"content\":\"コメント本文\"}]"
                + "}";
        Post post = gson.fromJson(jsonStr, Post.class);
        System.out.println("Post: タイトル=" + post.title
                            + ", 著者=" + post.author.fullname
                            + ", コメント件数=" + post.comments.size());

 */
    }

    @Test
    fun cityPatternTest() {
        val cityPattern = "(...??[都道府県])((?:旭川|伊達|石狩|盛岡|奥州|田村|南相馬|那須塩原|東村山|武蔵村山|羽村|十日町|上越|富山|野々市|大町|蒲郡|四日市|姫路|大和郡山|廿日市|下松|岩国|田川|大村|宮古|富良野|別府|佐伯|黒部|小諸|塩尻|玉野|周南)市|(?:余市|高市|[^市]{2,3}?)郡(?:玉村|大町|.{1,5}?)[町村]|(?:.{1,4}市)?[^町]{1,4}?区|.{1,7}?[市町村])(.+)".toRegex()

        val matchResult = cityPattern.matchEntire("福岡県福岡市西区愛宕浜2-1-8")
        assertNotNull(matchResult)
        assertEquals(matchResult?.groupValues?.get(2), "福岡市西区")
    }
}
