package com.tibco.bpm.cdm.core.logging;

import com.tibco.bpm.logging.cloud.annotations.message.CLFAuditMessage;
import com.tibco.bpm.logging.cloud.annotations.metadata.CloudMetaData;
import com.tibco.n2.logging.annotations.metadata.AuditProfile;
import com.tibco.n2.logging.context.interfaces.N2LFILogMessage;
import com.tibco.n2.logging.context.message.N2LogMessageContext;
import com.tibco.n2.logging.context.parser.N2LFContextContainer;
import com.tibco.n2.logging.enums.N2LFMessageCategory;
import com.tibco.n2.logging.metadata.common.CommonMetaData;

/**
 * Audit messages for success scenarios.
 * @author smorgan
 * @since 2019
 */
public enum CDMAuditMessages implements N2LFILogMessage
{
	//@formatter:off

	@CLFAuditMessage(messageId = "CDM_APPLICATION_DEPLOYED", auditProfiles= {AuditProfile.DEPLOYMENT },message = "Application Deployed", messageCategory = N2LFMessageCategory.DEPLOYMENT, attributeList = {
			CommonMetaData.APPLICATION_ID, CommonMetaData.MANAGED_OBJECT_ID})
	CDM_APPLICATION_DEPLOYED,

	@CLFAuditMessage(messageId = "CDM_APPLICATION_UNDEPLOYED", message = "Application Undeployed", auditProfiles= {AuditProfile.DEPLOYMENT },messageCategory = N2LFMessageCategory.DEPLOYMENT, attributeList = {
			CommonMetaData.APPLICATION_ID, CommonMetaData.MANAGED_OBJECT_ID})
	CDM_APPLICATION_UNDEPLOYED,

	@CLFAuditMessage(messageId = "CDM_CASE_CREATED", message = "Case Created", auditProfiles= {AuditProfile.CASE },messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CloudMetaData.CASE_STATE, CloudMetaData.CASE_PAYLOAD})
	CDM_CASE_CREATED,

	@CLFAuditMessage(messageId = "CDM_CASE_UPDATED_STATE_CHANGE", message = "Case Updated (State Change)",auditProfiles= {AuditProfile.CASE }, messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CloudMetaData.CASE_STATE, CloudMetaData.CASE_PAYLOAD})
	CDM_CASE_UPDATED_STATE_CHANGE,

	@CLFAuditMessage(messageId = "CDM_CASE_UPDATED_NO_STATE_CHANGE", message = "Case Updated (No State Change)", auditProfiles= {AuditProfile.CASE },messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CloudMetaData.CASE_STATE, CloudMetaData.CASE_PAYLOAD})
	CDM_CASE_UPDATED_NO_STATE_CHANGE,

	@CLFAuditMessage(messageId = "CDM_CASE_UNRECOGNISED_CONTENT_REMOVED", message = "Unrecognised content removed", auditProfiles= {AuditProfile.CASE },messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CloudMetaData.CASE_PAYLOAD})
	CDM_CASE_UNRECOGNISED_CONTENT_REMOVED,

	@CLFAuditMessage(messageId = "CDM_CASE_DELETED", message = "Case Deleted", auditProfiles= {AuditProfile.CASE },messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID})
	CDM_CASE_DELETED,

    @CLFAuditMessage(messageId = "CDM_CASE_IMPLICIT_UNLINKED", message = "Case Unlinked", auditProfiles = {
            AuditProfile.CASE }, messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
                    CommonMetaData.MANAGED_OBJECT_ID })
    CDM_CASE_IMPLICIT_UNLINKED,

	@CLFAuditMessage(messageId = "CDM_CASE_LINKED", message = "Case Linked", auditProfiles= {AuditProfile.CASE },messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CommonMetaData.ROLE_NAME, CommonMetaData.CASE_REFERENCE})
	CDM_CASE_LINKED,

	@CLFAuditMessage(messageId = "CDM_CASE_UNLINKED", message = "Case Unlinked",auditProfiles= {AuditProfile.CASE }, messageCategory = N2LFMessageCategory.CASE_DATA, attributeList = {
			CommonMetaData.MANAGED_OBJECT_ID, CommonMetaData.ROLE_NAME, CommonMetaData.CASE_REFERENCE})
	CDM_CASE_UNLINKED;

	//@formatter:on

	protected N2LFContextContainer<N2LogMessageContext> ctx = new N2LFContextContainer<N2LogMessageContext>(this);

    public N2LogMessageContext getContext()
	{
		return this.ctx.get();
	}

    public void setContext(N2LogMessageContext aValue)
	{
		this.ctx.set(aValue);
	}

}
