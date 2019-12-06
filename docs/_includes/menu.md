{% include codes.md %}

{% if lang == 'ar' %}{% include ar.md %}{% endif %}
{% if lang == 'de' %}{% include de.md %}{% endif %}
{% if lang == 'es' %}{% include es.md %}{% endif %}
{% if lang == 'fr' %}{% include fr.md %}{% endif %}
{% if lang == 'it' %}{% include it.md %}{% endif %}
{% if lang == 'zh' %}{% include zh.md %}{% endif %}
{% if lang == 'ru' %}{% include ru.md %}{% endif %}
{% if lang == 'pt' %}{% include pt.md %}{% endif %}
{% if lang == '' %}{% include index.md %}{% endif %}

<ul class="languagepicker roundborders large">
	<a href="#"><li>{{ full }}</li></a>
{% if full != 'English'%}<a href="index"><li>English</li></a>{% endif %}
{% if full != 'Русский'%}<a href="ru"><li>Русский</li></a>{% endif %}
{% if full != 'Français'%}<a href="fr"><li>Français</li></a>{% endif %}
{% if full != 'Deutsch'%}<a href="de"><li>Deutsch</li></a>{% endif %}
{% if full != 'Italiano'%}<a href="it"><li>Italiano</li></a>{% endif %}
{% if full != 'Portugal'%}<a href="pt"><li>Portugal</li></a>{% endif %}
{% if full != 'Español'%}<a href="es"><li>Español</li></a>{% endif %}
{% if full != 'العربية'%}<a href="ar"><li>العربية</li></a>{% endif %}
{% if full != '中文'%}<a href="zh"><li>中文</li></a>{% endif %}
</ul>
<p></p>
