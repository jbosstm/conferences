package cz.devconf2021.jta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ThiefWorkTest {

    @Test
    public void doThiefWork() {
        // List all,all cloak actions predefined into database on initialization
        given()
                .when().get("/thief")
                .then()
                .statusCode(200)
                .body(
                        containsString("Hide"),
                        containsString("Disguise"),
                        containsString("Wealthy"));

        // Create a new cloak action thingy
        given().urlEncodingEnabled(true)
                .contentType(ContentType.JSON)
                .when()
                .post("/thief/Run faster and faster")
                .then()
                .statusCode(201);

        // Verify new content
        given()
                .when().get("/thief")
                .then()
                .statusCode(200)
                .body(containsString("Run faster"));

    }

}
