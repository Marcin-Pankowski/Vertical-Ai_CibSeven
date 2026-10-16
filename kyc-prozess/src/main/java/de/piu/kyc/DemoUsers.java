package de.piu.kyc;

import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.identity.Group;
import org.cibseven.bpm.engine.identity.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Gruppe kyc-reviewers mit der Prüferin anna (Passwort anna), damit die manuellen Prüfungen auch in
 * der Tasklist von CIB seven erscheinen. Der Admin demo sieht sie über den Filter „Alle Aufgaben“.
 */
@Component
class DemoUsers {

    static final String GROUP = "kyc-reviewers";

    private final IdentityService identityService;

    DemoUsers(IdentityService identityService) {
        this.identityService = identityService;
    }

    @EventListener(ApplicationReadyEvent.class)
    void createUsers() {
        if (identityService.createGroupQuery().groupId(GROUP).count() == 0) {
            Group group = identityService.newGroup(GROUP);
            group.setName("KYC-Prüfer");
            group.setType("WORKFLOW");
            identityService.saveGroup(group);
        }
        if (identityService.createUserQuery().userId("anna").count() == 0) {
            User anna = identityService.newUser("anna");
            anna.setFirstName("Anna");
            anna.setLastName("Prüferin");
            anna.setPassword("anna");
            identityService.saveUser(anna);
            identityService.createMembership("anna", GROUP);
        }
    }
}
