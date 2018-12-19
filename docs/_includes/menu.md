{% assign find = false %}
{% assign lang = 'English' %}
 
{% if page.url contains 'ru.html' %}
{% assign find = true %}
{% assign lang = 'Русский' %}
{% include ru.md %}
{% endif %}


{% if page.url contains 'fr.html' %}
{% assign find = true %}
{% assign lang = 'Français' %}
{% include fr.md %}
{% endif %}

{% if page.url contains 'de.html' %}
{% assign find = true %}
{% assign lang = 'Deutsch' %}
{% include de.md %}
{% endif %}

{% if page.url contains 'it.html' %}
{% assign find = true %}
{% assign lang = 'Italiano' %}
{% include it.md %}
{% endif %}


{% if page.url contains 'es.html' %}
{% assign find = true %}
{% assign lang = 'Español' %}
{% include es.md %}
{% endif %}

{% if page.url contains 'pt.html' %}
{% assign find = true %}
{% assign lang = 'Portugal' %}
{% include pt.md %}
{% endif %}


{% if page.url contains 'zh.html' %}
{% assign find = true %}
{% assign lang = '中文' %}
{% include zh.md %}
{% endif %}

{% if page.url contains 'ar.html' %}
{% assign find = true %}
{% assign lang = 'العربية' %}
{% include ar.md %}
{% endif %}


{% if find == false %}
{% assign lang = 'English' %}
{% include index.md %}
{% endif %}


<ul class="languagepicker roundborders large">
	<a href="#"><li>{{ lang }}</li></a>
    <a href="index"><li>English</li></a>
    <a href="ru"><li>Русский</li></a>
	<a href="fr"><li>Français</li></a>
    <a href="de"><li>Deutsch</li></a>
    <a href="it"><li>Italiano</li></a>
    <a href="pt"><li>Portugal</li></a>
    <a href="es"><li>Español</li></a>
    <a href="ar"><li>العربية</li></a>
    <a href="zh"><li>中文</li></a>
</ul>

	       
	        