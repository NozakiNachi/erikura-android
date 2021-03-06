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
        // JSON??????String????????????
        String str = gson.fromJson("\"hello\"", String.class);
        System.out.println("String: " + str);

        // JSON??????Java??????????????????????????????
        User user = gson.fromJson("{\"email\":\"bob@jmail.com\",\"fullname\":\"Bob\"}", User.class);
        System.out.println("User: " + user.email + " / " + user.fullname);

        // JSON????????????????????????
        int[] array = gson.fromJson("[1, 2, 3]", int[].class);
        System.out.println("int[]: " + array[0] + ",???" + array[1] + ",???" + array[2]);

        // JSON??????List????????????
        List list = gson.fromJson("[\"hello\", \"hellohello\",\"hellohellohello\"]", List.class);
        System.out.println("List: " + list.get(0) + ",???" + list.get(1) + ",???" + list.get(2));

        // JSON????????????????????????List?????????Java??????????????????????????????
        String jsonStr = "{\"title\":\"??????????????????\",\"content\":\"??????????????????\","
                + "\"author\":{\"email\":\"bob@jmail.com\",\"fullname\":\"Bob\"},"
                + "\"comments\":[{\"author\":\"Tom\",\"content\":\"??????????????????\"}]"
                + "}";
        Post post = gson.fromJson(jsonStr, Post.class);
        System.out.println("Post: ????????????=" + post.title
                            + ", ??????=" + post.author.fullname
                            + ", ??????????????????=" + post.comments.size());

 */
    }

    @Test
    fun cityPatternTest() {
        val cityPattern = "(...??[????????????])((?:??????|??????|??????|??????|??????|??????|?????????|????????????|?????????|????????????|??????|?????????|??????|??????|?????????|??????|??????|?????????|??????|????????????|?????????|??????|??????|??????|??????|??????|?????????|??????|??????|??????|??????|??????|??????|??????)???|(?:??????|??????|[^???]{2,3}?)???(?:??????|??????|.{1,5}?)[??????]|(?:.{1,4}???)?[^???]{1,4}????|.{1,7}?[?????????])(.+)".toRegex()

        val matchResult = cityPattern.matchEntire("?????????????????????????????????2-1-8")
        assertNotNull(matchResult)
        assertEquals(matchResult?.groupValues?.get(2), "???????????????")
    }
}
