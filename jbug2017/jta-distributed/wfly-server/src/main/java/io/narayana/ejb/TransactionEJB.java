package io.narayana.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.xa.XAException;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TransactionEJB {

    @PersistenceContext(unitName = "primary")
    private EntityManager em;

    @Resource(mappedName = "java:/JmsXA")
    ConnectionFactory cf;

    @Resource(mappedName = "java:/jms/queue/example")
    private Queue queueExample;

    private Connection connection;
    private MessageProducer publisher;
    private Session session;

    public void doTransaction() throws XAException {
        System.out.println("Do transactional work now...");

        SimpleEntity employee= (SimpleEntity) em.find(SimpleEntity.class, "1");
        employee.setValue(employee.getValue() + 1);

        em.persist(employee);
        sendMessage("Hello");

        // throw new RuntimeException("Don't panic! The answer is 42.");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void sendMessage(String txt) {

        try {

            connection = cf.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            publisher = session.createProducer(queueExample);

            connection.start();

            TextMessage message = session.createTextMessage(txt);
            publisher.send(message);

        } catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("##########################################################");
            System.out.println("getCause().getSuppressed():" + exc.getCause().getSuppressed());
            System.out.println("getCause().getSuppressed().getCause():" + exc.getCause().getSuppressed()[0].getCause());
            System.out.println("##########################################################");
        } finally {

            if (publisher != null)
                try {
                    publisher.close();
                } catch (Exception ignore) {
                }
            if (session != null)
                try {
                    session.close();
                } catch (Exception ignore) {
                }
            if (connection != null)
                try {
                    connection.close();
                } catch (Exception ignore) {
                }
        }
    }
}
