package org.emdev.ui.actions.params;

/**
 * @author Alexander Kasatkin
 */
public class Constant extends AbstractActionParameter
{
    private Object m_value;

    /**
     * Constructor.
     *
     * @param name the parameter name
     * @param value the parameter value
     */
    public Constant(final String name, final Object value)
    {
        super(name);
        m_value = value;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.actions.IActionParameter#getValue()
     */
    public Object getValue()
    {
        return m_value;
    }

}
