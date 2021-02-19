package cz.devconf2021.stm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class STMResourceTest {

    @Test
    void testGet() {
        given()
                .when().get("/stm")
                .then()
                .statusCode(200);
    }

    @Test
    void testPost() {
        String responseString;

        makeBooking();
        responseString = makeBooking();

        assertThat(responseString, containsString("Action Number = 2"));
    }

    private String makeBooking() {
        return RestAssured.post("/stm").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .asString();
    }
}
