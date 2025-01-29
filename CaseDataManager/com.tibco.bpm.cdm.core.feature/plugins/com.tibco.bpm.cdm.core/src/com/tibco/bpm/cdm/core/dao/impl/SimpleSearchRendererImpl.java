package com.tibco.bpm.cdm.core.dao.impl;

import com.tibco.bpm.cdm.core.dao.SimpleSearchRenderer;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * SimpleSearchRenderer implementation for PostgreS using its Text Search capability
 * @author smorgan
 * @since 2019
 */
public class SimpleSearchRendererImpl implements SimpleSearchRenderer
{

    /*
     * This implementation has been updated using the postgress support for
     * query the json data for release 5.4.0. SELECT * FROM CDM_CASES_INT c
     * where casedata::json #>> '{customer,accounts,0,IBAN}' = 'iban 001'
     */
    private static final String TEMPLATE = "casedata::json #>> '{%s}'= ?"; //$NON-NLS-1$

    private static final String TEMPLATE_NUMERIC =
            "casedata::json #>> '{%s}'= ?"; //$NON-NLS-1$

    @Override
    public String render(StructuredType st) {
        return render(st, null); // $NON-NLS-1$
    }

    private String render(StructuredType st, String prefix) {

        StringBuilder buff = new StringBuilder();
        int i = 1, size = st.getSearchableAttributes().size();

        for (Attribute a : st.getAttributes()) {

            // Ignore the multi valued attributes for case manager search.
            if (a.getIsSearchable() && a.getIsArray()) {
                i++;
                continue;
            }
            if (a.getIsSearchable()) {
                AbstractType type = a.getTypeObject();
                String attrName = a.getName();
                if (prefix != null) {
                    attrName = prefix + "," + a.getName(); //$NON-NLS-1$
                }
                if ((i > 1 && i <= size) || (i == 1 & buff.length() != 0)) {
                    buff.append(" or "); //$NON-NLS-1$
                }
                if (type == BaseType.NUMBER) {
                    buff.append(String.format(TEMPLATE_NUMERIC, attrName));
                } else {
                    buff.append(String.format(TEMPLATE, attrName));
                }
                i++;
            } else if (a.getTypeObject() instanceof StructuredType
                    && !a.getIsArray()) {
                // Ignore the multi valued type attributes for case manager
                // search.
                String newPrefix = a.getName();
                if (prefix != null) {
                    newPrefix = prefix + "," + a.getName(); //$NON-NLS-1$
                }
                String searchString =
                        render((StructuredType) a.getTypeObject(), newPrefix);
                if (!searchString.isEmpty()) {
                    if (buff.length() != 0) {
                        buff.append(" or "); //$NON-NLS-1$
                    }
                    buff.append(searchString);
                }
            }

        }
        return buff.toString();
    }


    public int getNoOfSubstitutions(String render) {
        return render.split(" or ", 0).length; //$NON-NLS-1$
    }

}
