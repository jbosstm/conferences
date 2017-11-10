package io.narayana.ejb;

import java.util.Hashtable;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.transaction.UserTransaction;

import io.narayana.ejb.remote.Service;
 
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class RemoteEJBClient {
     
    @Resource
    private UserTransaction ut;

    @EJB
    private DBBean db;
    
    public void call(String dbIdUpdate) throws Throwable {

        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new javax.naming.InitialContext(props);
        Service service = (Service) context.lookup("ejb:/wfly-server//ServiceEJB!io.narayana.ejb.remote.Service");
         
        try {
            ut.begin();
            db.update(dbIdUpdate);
            service.exec();
            ut.commit();
        } catch (Throwable exc) {
            System.out.printf("##########################################################%n" +
                               "getMessage(): %s%n" + 
                              " getClass(): %s%n" +
                               "##########################################################%n",
                               exc.getMessage(), exc.getClass());
            
            // There's no active transaction at this point!
            //ut.rollback();
        }
    }
}
