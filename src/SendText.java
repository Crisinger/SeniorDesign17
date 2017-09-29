import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/* TWILIO ACCOUNT INFO
 *   email: colleen-gannon@uiowa.edu
 *   pw: RunHard2014DeWitt
 */

public class SendText {
	public static final String ACCOUNT_SID = "AC8bd26f32b53bc287501347668a869d46";
	public static final String AUTH_TOKEN = "4d6dc5bc3a45a81222a0580f9afaf36a";
	public static final String TWILIO_NUMBER = "+2244354812"; //texts sent "from" this number (spoofed)
	
	public static void sendTextMsg(String sendTo) {
		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
		
		//phone number formats MUST be in the form +5635933193
		Message message = Message.creator(new PhoneNumber("+" + sendTo), new PhoneNumber(TWILIO_NUMBER), "Temperature is outside of the critical range.").create();
		
		//System.out.println(message.getSid()); //can be used to check specifications on twilio.com
	}
}