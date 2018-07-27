package hpang.quickfix.executor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.management.JMException;
import javax.management.ObjectName;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

public class Executor {
	private final static Logger log = LoggerFactory.getLogger(Executor.class);
	private SocketAcceptor acceptor;

	private final JmxExporter jmxExporter = new JmxExporter();;
	private ObjectName connectorObjectName;

	public Executor(SessionSettings settings) throws ConfigError,
			FieldConvertError, JMException {
		init(settings);
	}

	private void start() throws RuntimeError, ConfigError {
		acceptor.start();
	}

	private void stop() {
		try {
			jmxExporter.getMBeanServer().unregisterMBean(connectorObjectName);
		} catch (Exception e) {
			log.error("Failed to unregister acceptor from JMX", e);
		}
		acceptor.stop();
	}

	public static void main(String[] args) throws Exception {
		try {
			InputStream inputStream = getSettingsInputStream(args);
			SessionSettings settings = new SessionSettings(inputStream);
			inputStream.close();

			Executor executor = new Executor(settings);
			executor.start();

			System.out.println("press <enter> to quit");
			System.in.read();

			executor.stop();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void init(SessionSettings settings) throws ConfigError,
			FieldConvertError, JMException {
		Application application = new Application(settings);
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();

		acceptor = new SocketAcceptor(application, messageStoreFactory,
				settings, logFactory, messageFactory);

		connectorObjectName = jmxExporter.register(acceptor);
		log.info("Acceptor registered with JMX, name={}", connectorObjectName);
	}

	private static InputStream getSettingsInputStream(String[] args)
			throws FileNotFoundException {
		InputStream inputStream = null;
		if (args.length == 0) {
			inputStream = Executor.class.getResourceAsStream("/executor.cfg");
		} else if (args.length == 1) {
			inputStream = new FileInputStream(args[0]);
		}
		if (inputStream == null) {
			System.out.println("usage: " + Executor.class.getName()
					+ " [configFile].");
			System.exit(1);
		}
		return inputStream;
	}
}
