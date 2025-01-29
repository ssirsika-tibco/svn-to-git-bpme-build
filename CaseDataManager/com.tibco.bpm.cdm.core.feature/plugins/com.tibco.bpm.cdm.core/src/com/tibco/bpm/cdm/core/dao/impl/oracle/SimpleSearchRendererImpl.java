package com.tibco.bpm.cdm.core.dao.impl.oracle;

import com.tibco.bpm.cdm.core.dao.SimpleSearchRenderer;
import com.tibco.bpm.da.dm.api.AbstractType;
import com.tibco.bpm.da.dm.api.Attribute;
import com.tibco.bpm.da.dm.api.BaseType;
import com.tibco.bpm.da.dm.api.StructuredType;

/**
 * SimpleSearchRenderer implementation to support oracle database.
 */
public class SimpleSearchRendererImpl implements SimpleSearchRenderer
{

    private static final String TEMPLATE = "json_value(casedata, '$.%s') = ? "; //$NON-NLS-1$

    private static final String TEMPLATE_NUMERIC =
            "json_value(casedata, '$.%s') = ? "; //$NON-NLS-1$

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
                    attrName = prefix + "." + a.getName(); //$NON-NLS-1$
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
                    newPrefix = prefix + "." + a.getName(); //$NON-NLS-1$
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


    @Override
    public int getNoOfSubstitutions(String render) {
        return render.split(" or ", 0).length; //$NON-NLS-1$
    }

}
