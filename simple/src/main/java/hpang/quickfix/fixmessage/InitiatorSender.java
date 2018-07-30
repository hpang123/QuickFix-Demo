package hpang.quickfix.fixmessage;

import hpang.quickfix.banzai.FixInitiator;
import hpang.quickfix.banzai.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.OrderQty;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;

@Component("initiatorSender")
public class InitiatorSender {
	
	@Autowired
	private FixInitiator fixInitiator;
	
	public void send(Order order) {
        quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(
                new ClOrdID(order.getID()), fixInitiator.sideToFIXSide(order.getSide()),
                new TransactTime(), fixInitiator.typeToFIXType(order.getType()));
        newOrderSingle.set(new OrderQty(order.getQuantity()));
        newOrderSingle.set(new Symbol(order.getSymbol()));
        newOrderSingle.set(new HandlInst('1'));
        fixInitiator.send(newOrderSingle, order.getSessionID());
    }

}
