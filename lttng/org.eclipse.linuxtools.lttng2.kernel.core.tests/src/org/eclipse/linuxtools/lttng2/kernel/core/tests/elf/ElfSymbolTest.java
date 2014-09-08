package org.eclipse.linuxtools.lttng2.kernel.core.tests.elf;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.cdt.utils.elf.Elf;
import org.eclipse.cdt.utils.elf.Elf.Section;
import org.junit.Test;

/**
 * Experiment the ELF parser API
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class ElfSymbolTest {

    @SuppressWarnings("javadoc")
    @Test
    public void testLoadElf() throws IOException {
        Elf elf = new Elf("/lib/x86_64-linux-gnu/libc.so.6");
        elf.loadSymbols();
        ArrayList<String> sectionNames = new ArrayList<>();
        for (Section sec: elf.getSections()) {
            sectionNames.add(sec.toString());
        }
        assertTrue(sectionNames.contains(".eh_frame"));
    }

}
