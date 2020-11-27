package common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.SecureRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import encryption.AES;
import encryption.HMAC;
import encryption.Hash;
import encryption.KeyPairManager;
import encryption.NonceManager;
import encryption.RandStrGenerator;
import encryption.VoteEncryption;
import exceptions.PEException;
import model.Message;
import model.VotePacket;
import utils.FileUtils;

/**
 * Unit test for simple App.
 */
public class AppTest 
{	
	
	@Before
	public void setup() {
		
   	}
	
	@After
	public void dismantle() {
		
	}
	
	@Test
	public void completeVoteEncryptionTest() throws Exception {
		KeyPair pair = KeyPairManager.genKeyPair();
	    
		Key Kpu = pair.getPublic();
		Key Kpr = pair.getPrivate();
		
		byte[] pub = Kpu.getEncoded();
		byte[] pr = Kpr.getEncoded();
		
		String sessionKey = RandStrGenerator.genSessionKey();
		
		String vote = "vote";
		String password = "password";
		int nonce = NonceManager.genSingleNonce();
		int modifiedNonce = NonceManager.getNonceResponse(nonce, 3);
		String encryptedNonce = AES.encryptNonce(modifiedNonce, sessionKey);
		
		
		VotePacket encryptedVotePacket = VoteEncryption.encrypt(vote, pub, encryptedNonce, sessionKey);
		
		//System.out.println(encryptedVotePacket.getEncryptedVote().length() + ";" + encryptedVotePacket.getEncryptedKi().length() + ";" + encryptedVotePacket.getEncryptedIV().length());
		
		byte[] encryptedPrivateKey = AES.encryptPrivateKey(pr, password);
		
		byte[] decryptedPrivateKey = AES.decryptPrivateKey(encryptedPrivateKey, password);		
		
		String decryptedVote = VoteEncryption.decrypt(encryptedVotePacket, decryptedPrivateKey);
		
		assertEquals(vote, decryptedVote);
	}
	
	@Test
	public void voteEncryptionTest() throws Exception {
		
		KeyPair pair = KeyPairManager.genKeyPair();
	    
		Key Kpu = pair.getPublic();
		Key Kpr = pair.getPrivate();
		
		byte[] pub = Kpu.getEncoded();
		byte[] pr = Kpr.getEncoded();

		String sessionKey = RandStrGenerator.genSessionKey();
		
		String vote = "vote";
		
		int nonce = NonceManager.genSingleNonce();
		int modifiedNonce = NonceManager.getNonceResponse(nonce, 3);
		String encryptedNonce = AES.encryptNonce(modifiedNonce, sessionKey);
		
		VotePacket encryptedVotePacket = VoteEncryption.encrypt(vote, pub, encryptedNonce, sessionKey);
		String decryptedVote = VoteEncryption.decrypt(encryptedVotePacket, pr);
		
		assertEquals(vote, decryptedVote);
		
	}
	
	@Test
	public void keyEncryptionTest() throws Exception {
		
		KeyPair pair = KeyPairManager.genKeyPair();
	    
		Key Kpr = pair.getPrivate();
		
		byte[] pr = Kpr.getEncoded();
		
		String password = "password";
		
		byte[] encryptedKey = AES.encryptPrivateKey(pr, password);
		byte[] decryptedKey = AES.decryptPrivateKey(encryptedKey, password);
		
		assertFalse(java.util.Arrays.equals(pr, encryptedKey));
		assertArrayEquals(pr, decryptedKey);
	}
	
	@Test
	public void hashedPasswordTest() throws Exception {
		int hashSize = 16;
		
		String password = "password";
		byte[] hashedPassword = Hash.computeHash(password, hashSize, "password");
		
		assertTrue(Hash.verifyHash(password, hashedPassword, "password"));
		
		String otherPassword = "otherPassword";
		byte[] hashedOtherPassword = Hash.computeHash(otherPassword, hashSize, "password");
		
		assertTrue(Hash.verifyHash(otherPassword, hashedOtherPassword, "password"));
		
		assertFalse(Hash.verifyHash(password, hashedOtherPassword, "password"));
		assertFalse(Hash.verifyHash(otherPassword, hashedPassword, "password"));
		
	}
	
	@Test
	public void HMACTest() throws Exception {
		String right = "correctPassword";
		String wrong = "wrongPassword";
		
		String encryptedNonce = "encryptedNonce";
		VotePacket packet = new VotePacket("encryptedVote", "encryptedSymKey", "encryptedIV", encryptedNonce);
		HMAC.sign(packet, right);
		
		assertTrue(HMAC.verify(packet, right));
		assertFalse(HMAC.verify(packet, wrong));
	}
	
	@Test
	public void urnSignatureTest() throws PEException {
		
		byte[] pr = RPTemp.getPrivate();
		byte[] pu = RPTemp.getPublic();
		
		String vote = "vote test 1";
		VotePacket packet = new VotePacket(vote, "encryptedSymKey", "encryptedIV", "encryptedNonce");
		
		VoteEncryption.signPacket(packet, pr);
		
		assertTrue(VoteEncryption.verifyPacketSignature(packet, pu));
		
		vote = "vote test 2";
		VotePacket otherPacket = new VotePacket(vote, "encryptedSymKey", "encryptedIV", "encryptedNonce");
		otherPacket.sign(packet.getSignature());
		
		assertTrue(!VoteEncryption.verifyPacketSignature(otherPacket, pu));
	}
	
	@Test
	public void nonceEncryptionTest() throws Exception {
		int nonce = new SecureRandom().nextInt();
		String password = RandStrGenerator.gen(10, 32);
		
		String encryptedNonce = AES.encryptNonce(nonce, password);		
		int decryptedNonce = AES.decryptNonce(encryptedNonce, password);
		
		assertEquals(nonce, decryptedNonce);
	}
	
	@Test
	public void sessionKeyGenerationTest() {
		String sessionKey = RandStrGenerator.genSessionKey();
		assertTrue(FileUtils.isSessionKey(sessionKey));
	}
	
	@Test
	public void messageTest() {
		Message message = new Message();
		
		String value = "value";
		
		try {
			message.verifyMessage(value, null, null, "test");
			assertTrue(false);
		}
		catch(PEException e){
			assertTrue(true);
		}
		
		message.setValue(value);
		
		try {
			message.verifyMessage(value, null, null, "test");
			assertTrue(true);
		}
		catch(PEException e){
			assertTrue(false);
		}
		
		String[] required = {"test"};
		Class<?>[] types = {Integer.class};
		
		try {
			message.verifyMessage(value, required, types, "test");
			assertTrue(false);
		}
		catch(PEException e){
			assertTrue(true);
		}
		
		int intTest = 5;
		message.setElement("test", intTest);
		
		try {
			message.verifyMessage(value, required, types, "test");
			assertTrue(true);
		}
		catch(PEException e){
			assertTrue(false);
		}
		
		assertEquals(intTest, (int) message.getElement("test"));
		
		String strTest = "cinque";
		message.setElement("test", strTest);
		
		try {
			message.verifyMessage(value, required, types, "test");
			assertTrue(false);
		}
		catch(PEException e){
			assertTrue(true);
		}
		
		Class<?>[] newTypes = {String.class};
		
		try {
			message.verifyMessage(value, required, newTypes, "test");
			assertTrue(true);
		}
		catch(PEException e){
			assertTrue(false);
		}
		
		assertEquals(strTest, message.getElement("test"));
		
		int[] intArrayTest = {1, 2, 3};
		
		message.setElement("test", intArrayTest);
		
		Class<?>[] newNewTypes = {int[].class};
		
		try {
			message.verifyMessage(value, required, newNewTypes, "test");
			assertTrue(true);
		}
		catch(PEException e){
			assertTrue(false);
		}
		
		assertEquals(intArrayTest, message.getElement("test"));
	}


}
