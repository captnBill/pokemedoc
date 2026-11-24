package com.example.pokemedoc.api;

import com.example.pokemedoc.api.CommonsImagePicker.Candidate;
import com.example.pokemedoc.game.MedicineImage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsImagePickerTest {

    private static Candidate file(int index, String title, String mime, int w, int h, String... categories) {
        String base = title.replace("File:", "").replace(' ', '_');
        return new Candidate(title, index, "https://upload.wikimedia.org/thumb/" + base,
                "https://commons.wikimedia.org/wiki/" + title.replace(' ', '_'), mime, w, h, List.of(categories));
    }

    private static Optional<MedicineImage> pick(String name, int minScore, Candidate... candidates) {
        return CommonsImagePicker.pick(name, List.of(candidates), minScore);
    }

    @Test
    void picksThePackagingPhotoAndSkipsTheMolecule() {
        Optional<MedicineImage> img = pick("Aspirin", 3,
                file(0, "File:Aspirin molecule.png", "image/png", 800, 600),
                file(1, "File:Aspirin-3D-balls.png", "image/png", 800, 600),
                file(2, "File:Aspirin Bayer box.jpg", "image/jpeg", 1200, 900));
        assertTrue(img.isPresent());
        assertTrue(img.get().url().endsWith("Aspirin_Bayer_box.jpg"));
    }

    @Test
    void aFileNamedLikeTheMedicineButWithoutAnyMedicineEvidenceIsRejected() {
        // e.g. a planet or a town called like a brand name
        assertTrue(pick("Mercury", 3,
                file(0, "File:Mercury in color.jpg", "image/jpeg", 1000, 1000, "Category:Planets")).isEmpty());
    }

    @Test
    void aMedicineCategoryIsEnoughEvidence() {
        assertTrue(pick("Zyrtec", 3,
                file(0, "File:Zyrtec.jpg", "image/jpeg", 600, 400, "Category:Antihistamine drugs")).isPresent());
    }

    @Test
    void theNameMustBeAWholeWordInTheFileName() {
        assertTrue(pick("Aspirin", 3,
                file(0, "File:Aspirinate box.jpg", "image/jpeg", 600, 400)).isEmpty());
    }

    @Test
    void moleculeCategoriesVetoAnImage() {
        assertTrue(pick("Ibuprofen", 3,
                file(0, "File:Ibuprofen tablets.jpg", "image/jpeg", 600, 400, "Category:Chemical structures")).isEmpty());
    }

    @Test
    void tinyFilesAndNonPhotoTypesAreRejected() {
        assertTrue(pick("Advil", 3,
                file(0, "File:Advil box.jpg", "image/jpeg", 120, 90),
                file(1, "File:Advil box.gif", "image/gif", 800, 600),
                file(2, "File:Advil box.svg", "image/svg+xml", 800, 600)).isEmpty());
    }

    @Test
    void packagingShotsWinOverPlainTabletsAndSearchOrderBreaksTies() {
        Optional<MedicineImage> img = pick("Zyrtec", 3,
                file(0, "File:Zyrtec tablets.jpg", "image/jpeg", 800, 600),
                file(1, "File:Zyrtec box.jpg", "image/jpeg", 800, 600),
                file(2, "File:Zyrtec package.jpg", "image/jpeg", 800, 600));
        assertTrue(img.isPresent());
        assertTrue(img.get().url().endsWith("Zyrtec_box.jpg"));   // same score as "package", ranked earlier
    }

    @Test
    void aHigherMinimumScoreDemandsBothFileNameAndCategoryEvidence() {
        Candidate nameOnly = file(0, "File:Zyrtec box.jpg", "image/jpeg", 800, 600);
        assertTrue(pick("Zyrtec", 6, nameOnly).isEmpty());
        assertEquals(1, CommonsImagePicker.pick("Zyrtec", List.of(
                file(0, "File:Zyrtec box.jpg", "image/jpeg", 800, 600, "Category:Medications")), 6).stream().count());
    }

    @Test
    void lastResortMoleculePrefersTheClassic2dDrawingAndFlagsItAsDiagram() {
        Optional<MedicineImage> img = CommonsImagePicker.pickMolecule(List.of(
                file(0, "File:Aspirin-3D-balls.png", "image/png", 800, 600),
                file(1, "File:Bayer logo.svg", "image/svg+xml", 400, 400),
                file(2, "File:Aspirin synthesis.png", "image/png", 800, 600),
                file(3, "File:Aspirin-skeletal.svg", "image/svg+xml", 300, 200)));
        assertTrue(img.isPresent());
        assertTrue(img.get().diagram());
        assertTrue(img.get().url().endsWith("Aspirin-skeletal.svg"));
    }

    @Test
    void lastResortMoleculeFindsNothingWhenThereIsNoStructureDrawing() {
        assertTrue(CommonsImagePicker.pickMolecule(List.of(
                file(0, "File:Aspirin pills.jpg", "image/jpeg", 800, 600),
                file(1, "File:Bayer logo.svg", "image/svg+xml", 400, 400))).isEmpty());
    }

    @Test
    void photosNeverCountAsDiagrams() {
        Optional<MedicineImage> img = pick("Aspirin", 3,
                file(0, "File:Aspirin Bayer box.jpg", "image/jpeg", 1200, 900));
        assertTrue(img.isPresent());
        assertEquals(false, img.get().diagram());
    }
}
