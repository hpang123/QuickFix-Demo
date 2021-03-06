/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package hpang.quickfix.banzai;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.BeginString;
import quickfix.field.BusinessRejectReason;
import quickfix.field.ClOrdID;
import quickfix.field.DeliverToCompID;
import quickfix.field.HandlInst;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Password;
import quickfix.field.RefMsgType;
import quickfix.field.RefSeqNum;
import quickfix.field.SenderCompID;
import quickfix.field.SessionRejectReason;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.Username;

public class BanzaiApplication implements Application {
	private final static Logger log = LoggerFactory
			.getLogger(BanzaiApplication.class);

	private final DefaultMessageFactory messageFactory = new DefaultMessageFactory();

	private boolean isAvailable = true;
	private boolean isMissingField;

	static private final TwoWayMap sideMap = new TwoWayMap();
	static private final TwoWayMap typeMap = new TwoWayMap();
	static private final TwoWayMap tifMap = new TwoWayMap();

	public BanzaiApplication() {
	}

	public void onCreate(SessionID sessionID) {
	}

	public void onLogon(SessionID sessionID) {
		log.info("onLogon:" + sessionID);
	}

	public void onLogout(SessionID sessionID) {
		log.info("onLogout:" + sessionID);
	}

	public void toAdmin(quickfix.Message message, SessionID sessionID) {
		if (message instanceof quickfix.fix44.Heartbeat) {
			log.info("Sent out Heartbeat...");
		}

		if (message instanceof quickfix.fix44.Logon) {
			message.setField(new Username("admin"));
			message.setField(new Password("password"));
		}

		log.info("toAdmin:" + sessionID + " message:" + message);
	}

	public void toApp(quickfix.Message message, SessionID sessionID)
			throws DoNotSend {
		log.info("toApp:" + sessionID + " message:" + message);
	}

	public void fromAdmin(quickfix.Message message, SessionID sessionID)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
			RejectLogon {
		log.info("fromAdmin:" + sessionID + " message:" + message);
	}

	public void fromApp(quickfix.Message message, SessionID sessionID)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
			UnsupportedMessageType {
		log.info("fromApp:" + sessionID + " message:" + message);
		try {
			SwingUtilities
					.invokeLater(new MessageProcessor(message, sessionID));
		} catch (Exception e) {
		}
	}

	public void send(Order order) {
		quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(
				new ClOrdID(order.getID()), sideToFIXSide(order.getSide()),
				new TransactTime(), typeToFIXType(order.getType()));
		newOrderSingle.set(new OrderQty(order.getQuantity()));
		newOrderSingle.set(new Symbol(order.getSymbol()));
		newOrderSingle.set(new HandlInst('1'));
		send(newOrderSingle, order.getSessionID());
	}

	private void send(quickfix.Message message, SessionID sessionID) {
		try {
			Session.sendToTarget(message, sessionID);
		} catch (SessionNotFound e) {
			System.out.println(e);
		}
	}

	public class MessageProcessor implements Runnable {
		private final quickfix.Message message;

		public MessageProcessor(quickfix.Message message, SessionID sessionID) {
			this.message = message;
		}

		public void run() {
			try {
				MsgType msgType = new MsgType();
				if (isAvailable) {
					if (isMissingField) {
						// For OpenFIX certification testing
						sendBusinessReject(
								message,
								BusinessRejectReason.CONDITIONALLY_REQUIRED_FIELD_MISSING,
								"Conditionally required field missing");
					} else if (message.getHeader().isSetField(
							DeliverToCompID.FIELD)) {
						// This is here to support OpenFIX certification
						sendSessionReject(message,
								SessionRejectReason.COMPID_PROBLEM);
					} else if (message.getHeader().getField(msgType)
							.valueEquals("8")) {
						// executionReport(message, sessionID);
						log.info("ExecutionReport...: " + message);
					} else if (message.getHeader().getField(msgType)
							.valueEquals("9")) {
						// cancelReject(message, sessionID);
						log.info("CancelReject...: " + message);
					} else {
						sendBusinessReject(message,
								BusinessRejectReason.UNSUPPORTED_MESSAGE_TYPE,
								"Unsupported Message Type");
					}
				} else {
					sendBusinessReject(message,
							BusinessRejectReason.APPLICATION_NOT_AVAILABLE,
							"Application not available");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sendSessionReject(Message message, int rejectReason)
			throws FieldNotFound, SessionNotFound {
		Message reply = createMessage(message, MsgType.REJECT);
		reverseRoute(message, reply);
		String refSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
		reply.setString(RefSeqNum.FIELD, refSeqNum);
		reply.setString(RefMsgType.FIELD,
				message.getHeader().getString(MsgType.FIELD));
		reply.setInt(SessionRejectReason.FIELD, rejectReason);
		Session.sendToTarget(reply);
	}

	private void sendBusinessReject(Message message, int rejectReason,
			String rejectText) throws FieldNotFound, SessionNotFound {
		Message reply = createMessage(message, MsgType.BUSINESS_MESSAGE_REJECT);
		reverseRoute(message, reply);
		String refSeqNum = message.getHeader().getString(MsgSeqNum.FIELD);
		reply.setString(RefSeqNum.FIELD, refSeqNum);
		reply.setString(RefMsgType.FIELD,
				message.getHeader().getString(MsgType.FIELD));
		reply.setInt(BusinessRejectReason.FIELD, rejectReason);
		reply.setString(Text.FIELD, rejectText);
		Session.sendToTarget(reply);
	}

	private Message createMessage(Message message, String msgType)
			throws FieldNotFound {
		return messageFactory.create(
				message.getHeader().getString(BeginString.FIELD), msgType);
	}

	private void reverseRoute(Message message, Message reply)
			throws FieldNotFound {
		reply.getHeader().setString(SenderCompID.FIELD,
				message.getHeader().getString(TargetCompID.FIELD));
		reply.getHeader().setString(TargetCompID.FIELD,
				message.getHeader().getString(SenderCompID.FIELD));
	}

	public Side sideToFIXSide(OrderSide side) {
		return (Side) sideMap.getFirst(side);
	}

	public OrderSide FIXSideToSide(Side side) {
		return (OrderSide) sideMap.getSecond(side);
	}

	public OrdType typeToFIXType(OrderType type) {
		return (OrdType) typeMap.getFirst(type);
	}

	public OrderType FIXTypeToType(OrdType type) {
		return (OrderType) typeMap.getSecond(type);
	}

	public TimeInForce tifToFIXTif(OrderTIF tif) {
		return (TimeInForce) tifMap.getFirst(tif);
	}

	public OrderTIF FIXTifToTif(TimeInForce tif) {
		return (OrderTIF) typeMap.getSecond(tif);
	}

	static {
		sideMap.put(OrderSide.BUY, new Side(Side.BUY));
		sideMap.put(OrderSide.SELL, new Side(Side.SELL));
		sideMap.put(OrderSide.SHORT_SELL, new Side(Side.SELL_SHORT));
		sideMap.put(OrderSide.SHORT_SELL_EXEMPT, new Side(
				Side.SELL_SHORT_EXEMPT));
		sideMap.put(OrderSide.CROSS, new Side(Side.CROSS));
		sideMap.put(OrderSide.CROSS_SHORT, new Side(Side.CROSS_SHORT));

		typeMap.put(OrderType.MARKET, new OrdType(OrdType.MARKET));
		typeMap.put(OrderType.LIMIT, new OrdType(OrdType.LIMIT));
		// typeMap.put(OrderType.STOP, new OrdType(OrdType.STOP_STOP_LOSS));
		typeMap.put(OrderType.STOP, new OrdType(OrdType.STOP));
		typeMap.put(OrderType.STOP_LIMIT, new OrdType(OrdType.STOP_LIMIT));

		tifMap.put(OrderTIF.DAY, new TimeInForce(TimeInForce.DAY));
		tifMap.put(OrderTIF.IOC, new TimeInForce(
				TimeInForce.IMMEDIATE_OR_CANCEL));
		tifMap.put(OrderTIF.OPG, new TimeInForce(TimeInForce.AT_THE_OPENING));
		tifMap.put(OrderTIF.GTC, new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
		tifMap.put(OrderTIF.GTX,
				new TimeInForce(TimeInForce.GOOD_TILL_CROSSING));
	}

	public boolean isMissingField() {
		return isMissingField;
	}

	public void setMissingField(boolean isMissingField) {
		this.isMissingField = isMissingField;
	}

	public boolean isAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
}
