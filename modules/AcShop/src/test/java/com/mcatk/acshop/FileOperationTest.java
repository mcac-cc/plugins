package com.mcatk.acshop;

import com.mcatk.acshop.shop.Shops;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.FileReader;
import java.io.FileWriter;

import static org.mockito.Mockito.*;

public class FileOperationTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void closesTheShopFileAfterEachSaveAndLoad() throws Exception {
        AcShop plugin = mock(AcShop.class);
        when(plugin.getDataFolder()).thenReturn(folder.getRoot());
        folder.newFile("shopsFile.json");
        try (MockedStatic<AcShop> current = mockStatic(AcShop.class);
             MockedConstruction<FileWriter> writers = mockConstruction(FileWriter.class);
             MockedConstruction<FileReader> readers = mockConstruction(FileReader.class, (reader, context) ->
                     when(reader.read(any(char[].class), anyInt(), anyInt())).thenReturn(-1))) {
            current.when(AcShop::getPlugin).thenReturn(plugin);
            FileOperation files = new FileOperation();
            files.saveShops(new Shops());
            files.loadShops();
            verify(writers.constructed().get(0)).close();
            verify(readers.constructed().get(0)).close();
        }
    }
}
