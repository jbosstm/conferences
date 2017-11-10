package io.narayana.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class DBBean {
    private static final String INSERT_SQL = "INSERT INTO simple_entity (id,value) VALUES ('1', 1)";
    private static final String UPDATE_SQL = "UPDATE simple_entity SET value = value + 1 WHERE id = ?";

    @Resource(lookup = "java:jboss/xa-datasource")
    private DataSource ds;

    public void update(String id) {
        try {
            PreparedStatement preparedStatement = null;
            Connection dbConnection = ds.getConnection();
            preparedStatement = dbConnection.prepareStatement(UPDATE_SQL);
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Trouble to run update query", e);
        }
    }
}
