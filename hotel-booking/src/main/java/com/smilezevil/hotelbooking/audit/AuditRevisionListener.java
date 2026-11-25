package com.smilezevil.hotelbooking.audit;

import org.hibernate.envers.RevisionListener;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setChangedBy(CurrentAuditor.require());
    }
}