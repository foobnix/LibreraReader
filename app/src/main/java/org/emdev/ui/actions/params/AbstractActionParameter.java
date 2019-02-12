package org.emdev.ui.actions.params;

import org.emdev.ui.actions.IActionParameter;

/**
 * @author Alexander Kasatkin
 */
public abstract class AbstractActionParameter implements IActionParameter
{
    private String m_name;

    /**
     * Constructor.
     *
     * @param name the name
     */
    protected AbstractActionParameter(final String name)
    {
        super();
        m_name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.actions.IActionParameter#getName()
     */
    public String getName()
    {
        return m_name;
    }
}
