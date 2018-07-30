package hpang.quickfix;

import static org.junit.Assert.assertNotNull;
import hpang.quickfix.banzai.FixInitiator;
import hpang.quickfix.banzai.Order;
import hpang.quickfix.banzai.OrderSide;
import hpang.quickfix.banzai.OrderTIF;
import hpang.quickfix.banzai.OrderType;
import hpang.quickfix.config.TestConfig;
import hpang.quickfix.executor.FixAcceptor;
import hpang.quickfix.fixmessage.InitiatorSender;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class FixTest {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private FixAcceptor fixAcceptor;
	
	@Autowired
	private FixInitiator fixInitiator;
	
	@Autowired
	private InitiatorSender initiatorSender;
	
	@Before
	public void setup() {}

	@After
	public void reset() {}
	
	@Test
	public void testAcceptorAndInitiator() {
		
		assertNotNull(fixAcceptor);
		assertNotNull(fixInitiator);
	}
	
	@Test
	public void testSendOrder() {
		log.info("Logon...");
		fixInitiator.logon();
        
       
        log.info("Send order... ");
        sendOrder(1);
        
        try{
        	Thread.sleep(5000);
        }
        catch(Exception e){
        	
        }
        
        log.info("Logout...");
        fixInitiator.logout();
	}


	private void sendOrder(int i){
    	Order order = new Order();
        order.setSide(OrderSide.BUY);
        order.setType((OrderType.MARKET));
        order.setTIF((OrderTIF.DAY));

        order.setSymbol("BAML" + i);
        order.setQuantity(100*i);
        order.setOpen(order.getQuantity());
        
        order.setSessionID(fixInitiator.getInitiator().getSessions().get(0));

        initiatorSender.send(order);
        
    }
}
