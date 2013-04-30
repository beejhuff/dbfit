package dbfit.util.crypto;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;

import org.mockito.Mock;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import dbfit.util.MockitoTestBase;


public class CryptoAppTest extends MockitoTestBase {

    @Mock private CryptoService mockedCryptoService;
    @Mock private CryptoKeyStoreManager mockedKSManager;
    @Mock private CryptoKeyStoreManagerFactory mockedKSManagerFactory;
    @Mock private CryptoServiceFactory mockedCryptoServiceFactory;

    @Rule public TemporaryFolder tempKeyStoreFolder = new TemporaryFolder();
    @Rule public TemporaryFolder tempKeyStoreFolder2 = new TemporaryFolder();

    private String getTempKeyStorePath() throws IOException {
        return tempKeyStoreFolder.getRoot().getCanonicalPath();
    }

    private String getTempKeyStore2Path() throws IOException {
        return tempKeyStoreFolder2.getRoot().getCanonicalPath();
    }

    private CryptoApp createCryptoApp() {
        return new CryptoApp(mockedKSManagerFactory);
    }

    @Before
    public void prepare() throws IOException {
        when(mockedKSManagerFactory.newInstance()).thenReturn(mockedKSManager);
        when(mockedKSManagerFactory.newInstance(any(File.class))).thenReturn(mockedKSManager);
        when(mockedCryptoServiceFactory.getCryptoService()).thenReturn(mockedCryptoService);

        CryptoAdmin.setKSManagerFactory(mockedKSManagerFactory);
        CryptoAdmin.setCryptoServiceFactory(mockedCryptoServiceFactory);

        System.setProperty("dbfit.keystore.path", getTempKeyStorePath());
    }

    @After
    public void tearDown() {
        CryptoAdmin.setCryptoServiceFactory(null);
        CryptoAdmin.setKSManagerFactory(null);
        CryptoAdmin.setCryptoKeyServiceFactory(null);
    }

    @Test
    public void createKeyStoreInDefaultLocationTest() throws Exception {
        execApp("-createKeyStore");

        verify(mockedKSManagerFactory).newInstance();
        verify(mockedKSManager).initKeyStore();
    }

    @Test
    public void createKeyStoreInCustomLocationTest() throws Exception {
        execApp("-createKeyStore", getTempKeyStore2Path());

        verify(mockedKSManagerFactory).newInstance(tempKeyStoreFolder2.getRoot());
        verify(mockedKSManager).initKeyStore();
    }

    @Test
    public void encryptPasswordTest() throws Exception {
        when(mockedKSManager.keyStoreExists()).thenReturn(true);
        String password = "Demo Password CLI";

        execApp("-encryptPassword", password);

        verify(mockedCryptoService).encrypt(password);
    }

    @Test
    public void shouldCreateKeyStoreBeforeGettingKeyService() throws Exception {
        when(mockedKSManager.keyStoreExists()).thenReturn(false);
        String password = "Demo Password CLI 2";

        execApp("-encryptPassword", password);

        InOrder inOrder = inOrder(
                mockedKSManagerFactory, mockedKSManager,
                mockedCryptoServiceFactory, mockedCryptoService);

        inOrder.verify(mockedKSManagerFactory).newInstance();
        inOrder.verify(mockedKSManager).initKeyStore();
        inOrder.verify(mockedCryptoServiceFactory).getCryptoService();
        inOrder.verify(mockedCryptoService).encrypt(password);
    }

    @Test
    public void shouldReturnNonZeroOnEmptyArgs() throws Exception {
        assertEquals(1, execApp());
    }

    @Test
    public void shouldReturnOneOnInvalidCommand() throws Exception {
        assertEquals(1, execApp("-non-existing-command"));
        assertEquals(1, execApp("another invalid command"));
    }

    @Test
    public void shouldReturnTwoOnInvalidNumberOfOptions() throws Exception {
        assertEquals(2, execApp("-encryptPassword", "too", "many", "args"));
        assertEquals(2, execApp("-createKeyStore", "too", "many"));
    }

    private int execApp(String... args) throws Exception {
        return createCryptoApp().execute(args);
    }
}

