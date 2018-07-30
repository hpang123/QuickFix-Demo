package hpang.quickfix.config;

import hpang.quickfix.banzai.FixInitiator;
import hpang.quickfix.executor.FixAcceptor;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import quickfix.SessionSettings;



@Configuration
@ComponentScan({ "hpang.quickfix.*" })
public class FIXConfig {
	
	@Bean 
	public SessionSettings acceptorSessionSettings(){
		//InputStream inputStream = Executor.class.getResourceAsStream("/executor1.cfg");
		//ClassPathResource classPathResource = new ClassPathResource("executor.cfg");
		//InputStream inputStream = classPathResource.getInputStream();
		
		try{
			
			//SessionSettings sessionSettings = new SessionSettings(inputStream);
			//inputStream.close();
			return new SessionSettings("executor.cfg");
		}
		catch(Exception e){
			throw new BeanCreationException("acceptorSessionSettings", "Failed to create a Acceptor SessionSettings", e);
		}
		
	}
	
	@Bean 
	public FixAcceptor fixAcceptor(){
		try{
			return new FixAcceptor(acceptorSessionSettings());
		}
		catch(Exception e){
			throw new BeanCreationException("fixAcceptor", "Failed to create a fixAcceptor", e);
		}
	}
	
	@Bean 
	public SessionSettings initiatorSessionSettings(){
		try{
			return new SessionSettings("banzai.cfg");
		}
		catch(Exception e){
			throw new BeanCreationException("acceptorSessionSettings", "Failed to create a Acceptor SessionSettings", e);
		}
	}
	
	@Bean 
	public FixInitiator fixInitiator(){
		try{
			return new FixInitiator(initiatorSessionSettings());
		}
		catch(Exception e){
			throw new BeanCreationException("fixInitiator", "Failed to create a fixInitiator", e);
		}
	}
}
