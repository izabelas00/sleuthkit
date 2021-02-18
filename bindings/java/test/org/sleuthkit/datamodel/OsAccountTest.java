/*
 * Sleuth Kit Data Model
 *
 * Copyright 2021 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.datamodel;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * Tests OsAccount apis.
 *
 */
public class OsAccountTest {
	
	private static final Logger LOGGER = Logger.getLogger(OsAccountTest.class.getName());

	private static SleuthkitCase caseDB;

	private final static String TEST_DB = "OsAccountApiTest.db";


	private static String dbPath = null;
	
	private static Image image;
	
	private static FileSystem fs = null;

	public OsAccountTest (){

	}
	
	@BeforeClass
	public static void setUpClass() {
		String tempDirPath = System.getProperty("java.io.tmpdir");
		try {
			dbPath = Paths.get(tempDirPath, TEST_DB).toString();

			// Delete the DB file, in case
			java.io.File dbFile = new java.io.File(dbPath);
			dbFile.delete();
			if (dbFile.getParentFile() != null) {
				dbFile.getParentFile().mkdirs();
			}

			// Create new case db
			caseDB = SleuthkitCase.newCase(dbPath);

			SleuthkitCase.CaseDbTransaction trans = caseDB.beginTransaction();

			image = caseDB.addImage(TskData.TSK_IMG_TYPE_ENUM.TSK_IMG_TYPE_DETECT, 512, 1024, "", Collections.emptyList(), "America/NewYork", null, null, null, "first", trans);

			fs = caseDB.addFileSystem(image.getId(), 0, TskData.TSK_FS_TYPE_ENUM.TSK_FS_TYPE_RAW, 0, 0, 0, 0, 0, "", trans);

			trans.commit();


			System.out.println("OsAccount Test DB created at: " + dbPath);
		} catch (TskCoreException ex) {
			LOGGER.log(Level.SEVERE, "Failed to create new case", ex);
		}
	}


	@AfterClass
	public static void tearDownClass() {

	}
	
	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test 
	public void hostTests() throws TskCoreException {
		
		try {
			String HOSTNAME1 = "host11";
			
			// Test: create a host
			Host host1 = caseDB.getHostManager().createHost(HOSTNAME1);
			assertEquals(host1.getName().equalsIgnoreCase(HOSTNAME1), true );
			
			
			// Test: get a host we just created.
			Optional<Host> optionalhost1 = caseDB.getHostManager().getHost(HOSTNAME1);
			assertEquals(optionalhost1.isPresent(), true );
			
			
			String HOSTNAME2 = "host22";
			
			// Get a host not yet created
			Optional<Host> optionalhost2 = caseDB.getHostManager().getHost(HOSTNAME2);
			assertEquals(optionalhost2.isPresent(), false );
			
			
			// now create the second host
			Host host2 = caseDB.getHostManager().createHost(HOSTNAME2);
			assertEquals(host2.getName().equalsIgnoreCase(HOSTNAME2), true );
			
			
			// now get it again, should be found this time
			optionalhost2 = caseDB.getHostManager().getHost(HOSTNAME2);
			assertEquals(optionalhost2.isPresent(), true);
			
			// create a host that already exists - should transperently return the existting host.
			Host host2_2 = caseDB.getHostManager().createHost(HOSTNAME2);
			assertEquals(host2_2.getName().equalsIgnoreCase(HOSTNAME2), true );
			
		}
		catch(Exception ex) {
			
		}
	
	}
	@Test
	public void osAccountRealmTests() throws TskCoreException {
		
		try {
		// TEST: create a DOMAIN realm 
		
		String HOSTNAME1 = "host1";
		Host host1 = caseDB.getHostManager().createHost(HOSTNAME1);
			
		String realmName1 = "basis";
		String realmSID1 =  "S-1-5-21-1111111111-2222222222-3333333333";
		String realmAddr1 = "S-1-5-21-1111111111-2222222222";	
		
		OsAccountRealm domainRealm1 = caseDB.getOsAccountRealmManager().createWindowsRealm(realmSID1, realmName1, host1, OsAccountRealm.RealmScope.DOMAIN);
		
		assertEquals(domainRealm1.getRealmName().orElse("").equalsIgnoreCase(realmName1), true );
		assertEquals(domainRealm1.getScopeConfidence(), OsAccountRealm.ScopeConfidence.KNOWN);
		assertEquals(domainRealm1.getRealmAddr().orElse(null), realmAddr1); 
		
	
		//TEST: create a new LOCAL realm with a single host
		String realmSID2 = "S-1-5-18-2033736216-1234567890-5432109876";
		String realmAddr2 = "S-1-5-18-2033736216-1234567890";	
		String realmName2 = "win-raman-abcd";
		String hostName2 = "host2";
		
		Host host2 = caseDB.getHostManager().createHost(hostName2);
		OsAccountRealm localRealm2 = caseDB.getOsAccountRealmManager().createWindowsRealm(realmSID2, null, host2, OsAccountRealm.RealmScope.LOCAL);
		assertEquals(localRealm2.getRealmAddr().orElse("").equalsIgnoreCase(realmAddr2), true );
		assertEquals(localRealm2.getScopeHost().orElse(null).getName().equalsIgnoreCase(hostName2), true);
		
		// update the a realm name on a existing realm.
		localRealm2.setRealmName(realmName2);
		OsAccountRealm updatedRealm2 = caseDB.getOsAccountRealmManager().updateRealm(localRealm2);
		assertEquals(updatedRealm2.getRealmAddr().orElse("").equalsIgnoreCase(realmAddr2), true );
		assertEquals(updatedRealm2.getRealmName().orElse("").equalsIgnoreCase(realmName2), true );
		
		
		
		// TEST get an existing DOMAIN realm - new SID  on a new host but same sub authority as previously created realm
		String realmSID3 = realmAddr1 + "-88888888";
		
		String hostName3 = "host3";
		Host host3 = caseDB.getHostManager().createHost(hostName3);
		
		// expect this to return realm1
		Optional<OsAccountRealm> existingRealm3 = caseDB.getOsAccountRealmManager().getWindowsRealm(realmSID3, null, host3); 
		assertEquals(existingRealm3.isPresent(), true);
		assertEquals(existingRealm3.get().getRealmAddr().orElse("").equalsIgnoreCase(realmAddr1), true );
		assertEquals(existingRealm3.get().getRealmName().orElse("").equalsIgnoreCase(realmName1), true );
		
		
		// TEST get a existing LOCAL realm by addr, BUT on a new referring host.
		String hostName4 = "host4";
		Host host4 = caseDB.getHostManager().createHost(hostName4);
		
		// Although the realm exists with this addr, it should  NOT match since the host is different from what the realm was created with
		Optional<OsAccountRealm> realm4 = caseDB.getOsAccountRealmManager().getWindowsRealm(realmSID2, null, host4);
		
		assertEquals(realm4.isPresent(), false);
				
		}
		finally {

		}
		
		
	}
	
	@Test
	public void basicOsAccountTests() throws TskCoreException {

		try {
			//String ownerUid1 = "S-1-5-32-544"; // special short SIDS not handled yet
			
			// Create an account in a local scoped realm.
			
			String ownerUid1 = "S-1-5-32-111111111-222222222-3333333333-0001";
			String realmName1 = "realm1";
			
			String hostname1 = "host1";
			Host host1 = caseDB.getHostManager().createHost(hostname1);
			
			OsAccountRealm localRealm1 = caseDB.getOsAccountRealmManager().createWindowsRealm(ownerUid1, realmName1, host1, OsAccountRealm.RealmScope.LOCAL);
			OsAccount osAccount1 = caseDB.getOsAccountManager().createWindowsAccount(ownerUid1, null, realmName1, host1, OsAccountRealm.RealmScope.LOCAL);
			
			assertEquals(osAccount1.isAdmin(), false);
			assertEquals(osAccount1.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(ownerUid1), true);
			assertEquals(osAccount1.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName1), true);
			
			
			// Let's update osAccount1
			String fullName1 = "Johnny Depp";
			Long creationTime1 = 1611858618L;
			boolean isChanged = osAccount1.setCreationTime(creationTime1);
			assertEquals(isChanged, true);
			
			osAccount1.setFullName(fullName1);
			osAccount1.setIsAdmin(true);
			
			
			osAccount1 = caseDB.getOsAccountManager().updateAccount(osAccount1);
			assertEquals(osAccount1.getCreationTime().orElse(null), creationTime1);
			assertEquals(osAccount1.getFullName().orElse(null).equalsIgnoreCase(fullName1), true );
			
			
			// now try and create osAccount1 again - it should return the existing account
			OsAccount osAccount1_copy1 = caseDB.getOsAccountManager().createWindowsAccount(ownerUid1, null, realmName1, host1, OsAccountRealm.RealmScope.LOCAL);
			
			
			assertEquals(osAccount1_copy1.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(ownerUid1), true);
			assertEquals(osAccount1_copy1.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName1), true);
			
			
			assertEquals(osAccount1_copy1.isAdmin(), true); // should be true now
			assertEquals(osAccount1_copy1.getFullName().orElse("").equalsIgnoreCase(fullName1), true);
			assertEquals(osAccount1.getCreationTime().orElse(null), creationTime1);
			
			
			// Create two new accounts on a new domain realm
			String ownerUid2 = "S-1-5-21-725345543-854245398-1060284298-1003";
			String ownerUid3 = "S-1-5-21-725345543-854245398-1060284298-1004";
	
			String realmName2 = "basis";
			
			String hostname2 = "host2";
			String hostname3 = "host3";
			Host host2 = caseDB.getHostManager().createHost(hostname2);
			Host host3 = caseDB.getHostManager().createHost(hostname3);
		
			OsAccountRealm domainRealm1 = caseDB.getOsAccountRealmManager().createWindowsRealm(ownerUid2, realmName2, host2, OsAccountRealm.RealmScope.DOMAIN);
		
			// create accounts in this domain scoped realm
			OsAccount osAccount2 = caseDB.getOsAccountManager().createWindowsAccount(ownerUid2, null, realmName2, host2, OsAccountRealm.RealmScope.DOMAIN);
			OsAccount osAccount3 = caseDB.getOsAccountManager().createWindowsAccount(ownerUid3, null, realmName2, host3, OsAccountRealm.RealmScope.DOMAIN);
			
			assertEquals(osAccount2.isAdmin(), false);
			assertEquals(osAccount2.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(ownerUid2), true);
			assertEquals(osAccount2.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName2), true);
			
			
			assertEquals(osAccount3.isAdmin(), false);
			assertEquals(osAccount3.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(ownerUid3), true);
			assertEquals(osAccount3.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName2), true);
			
		}
		
		finally {
			
		}

	}
	
	
	@Test
	public void windowsSpecialAccountTests() throws TskCoreException {

		try {
			
			String SPECIAL_WINDOWS_REALM_ADDR = "SPECIAL_WINDOWS_ACCOUNTS";
			
			
			
			// TEST: create accounts with a "short" sid
			{
				String hostname1 = "host111";
				Host host1 = caseDB.getHostManager().createHost(hostname1);

				String realmName1 = "realmName111";
				String sid1 = "S-1-5-32-544"; // builtin Administrators
				String sid2 = "S-1-5-32-545"; //  builtin Users
				String sid3 = "S-1-5-32-546"; //  builtin Guests
				String realmAddr1 = "S-1-5-32";

				OsAccount osAccount1 = caseDB.getOsAccountManager().createWindowsAccount(sid1, null, realmName1, host1, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount osAccount2 = caseDB.getOsAccountManager().createWindowsAccount(sid2, null, realmName1, host1, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount osAccount3 = caseDB.getOsAccountManager().createWindowsAccount(sid3, null, realmName1, host1, OsAccountRealm.RealmScope.UNKNOWN);

				assertEquals(osAccount1.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName1), true);
				assertEquals(osAccount1.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(realmAddr1), true);
				assertEquals(osAccount1.isAdmin(), false);
				assertEquals(osAccount1.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(sid1), true);

				assertEquals(osAccount2.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName1), true);
				assertEquals(osAccount2.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(realmAddr1), true);
				assertEquals(osAccount2.isAdmin(), false);
				assertEquals(osAccount2.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(sid2), true);

				assertEquals(osAccount3.getRealm().getRealmName().orElse("").equalsIgnoreCase(realmName1), true);
				assertEquals(osAccount3.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(realmAddr1), true);
				assertEquals(osAccount3.isAdmin(), false);
				assertEquals(osAccount3.getUniqueIdWithinRealm().orElse("").equalsIgnoreCase(sid3), true);
			}
			
			
			
		
			// TEST create accounts with special SIDs on host2
			{
				String hostname2 = "host222";
				Host host2 = caseDB.getHostManager().createHost(hostname2);

				String specialSid1 = "S-1-5-18";
				String specialSid2 = "S-1-5-19";
				String specialSid3 = "S-1-5-20";

				OsAccount specialAccount1 = caseDB.getOsAccountManager().createWindowsAccount(specialSid1, null, null, host2, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount2 = caseDB.getOsAccountManager().createWindowsAccount(specialSid2, null, null, host2, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount3 = caseDB.getOsAccountManager().createWindowsAccount(specialSid3, null, null, host2, OsAccountRealm.RealmScope.UNKNOWN);

				assertEquals(specialAccount1.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount2.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount3.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
			}
			
			
			// TEST create accounts with special SIDs on host3 - should create their own realm 
			{
				String hostname3 = "host333";
				Host host3 = caseDB.getHostManager().createHost(hostname3);

				String specialSid1 = "S-1-5-18";
				String specialSid2 = "S-1-5-19";
				String specialSid3 = "S-1-5-20";

				OsAccount specialAccount1 = caseDB.getOsAccountManager().createWindowsAccount(specialSid1, null, null, host3, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount2 = caseDB.getOsAccountManager().createWindowsAccount(specialSid2, null, null, host3, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount3 = caseDB.getOsAccountManager().createWindowsAccount(specialSid3, null, null, host3, OsAccountRealm.RealmScope.UNKNOWN);

				assertEquals(specialAccount1.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount2.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount3.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				
				// verify a new local realm with host3 was created for these account even they've been seen previously on another host
				assertEquals(specialAccount1.getRealm().getScopeHost().orElse(null).getName().equalsIgnoreCase(hostname3), true);
				assertEquals(specialAccount1.getRealm().getScopeHost().orElse(null).getName().equalsIgnoreCase(hostname3), true);
				assertEquals(specialAccount1.getRealm().getScopeHost().orElse(null).getName().equalsIgnoreCase(hostname3), true);
			}

			
			// Test some other special account.
			{
				String hostname4 = "host444";
				Host host4 = caseDB.getHostManager().createHost(hostname4);

				String specialSid1 = "S-1-5-80-3696737894-3623014651-202832235-645492566-13622391";
				String specialSid2 = "S-1-5-82-4003674586-223046494-4022293810-2417516693-151509167";
				String specialSid3 = "S-1-5-90-0-2";
				String specialSid4 = "S-1-5-96-0-3";
				

				OsAccount specialAccount1 = caseDB.getOsAccountManager().createWindowsAccount(specialSid1, null, null, host4, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount2 = caseDB.getOsAccountManager().createWindowsAccount(specialSid2, null, null, host4, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount3 = caseDB.getOsAccountManager().createWindowsAccount(specialSid3, null, null, host4, OsAccountRealm.RealmScope.UNKNOWN);
				OsAccount specialAccount4 = caseDB.getOsAccountManager().createWindowsAccount(specialSid4, null, null, host4, OsAccountRealm.RealmScope.UNKNOWN);
				

				assertEquals(specialAccount1.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount2.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount3.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				assertEquals(specialAccount4.getRealm().getRealmAddr().orElse("").equalsIgnoreCase(SPECIAL_WINDOWS_REALM_ADDR), true);
				
				
			}
			
		}
		
		finally {
			
		}

	}
	
	
	@Test
	public void osAccountInstanceTests() throws TskCoreException {

		String ownerUid1 = "S-1-5-32-111111111-222222222-3333333333-0001";
		String realmName1 = "realm1111";

		String hostname1 = "host1111";
		Host host1 = caseDB.getHostManager().createHost(hostname1);

		OsAccountRealm localRealm1 = caseDB.getOsAccountRealmManager().createWindowsRealm(ownerUid1, realmName1, host1, OsAccountRealm.RealmScope.LOCAL);
		OsAccount osAccount1 = caseDB.getOsAccountManager().createWindowsAccount(ownerUid1, null, realmName1, host1, OsAccountRealm.RealmScope.LOCAL);

		// Test: add an instance
		caseDB.getOsAccountManager().createOsAccountInstance(osAccount1, host1, image.getId(), OsAccount.OsAccountInstanceType.PERFORMED_ACTION_ON);

		// Test: add an existing instance - should be a no-op.
		caseDB.getOsAccountManager().createOsAccountInstance(osAccount1, host1, image.getId(), OsAccount.OsAccountInstanceType.PERFORMED_ACTION_ON);

		// Test: create account instance on a new host
		String hostname2 = "host2222";
		Host host2 = caseDB.getHostManager().createHost(hostname2);
		caseDB.getOsAccountManager().createOsAccountInstance(osAccount1, host2, image.getId(), OsAccount.OsAccountInstanceType.REFERENCED_ON);

	}
	
	
}