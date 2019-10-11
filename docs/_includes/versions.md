{% assign lang = '' %}
 
{% if page.url contains 'ar.html' %}{% assign lang = 'ar' %}{% endif %}
{% if page.url contains 'de.html' %}{% assign lang = 'de' %}{% endif %}
{% if page.url contains 'es.html' %}{% assign lang = 'es' %}{% endif %}
{% if page.url contains 'fr.html' %}{% assign lang = 'fr' %}{% endif %}
{% if page.url contains 'it.html' %}{% assign lang = 'it' %}{% endif %}
{% if page.url contains 'ru.html' %}{% assign lang = 'ru' %}{% endif %}
{% if page.url contains 'zh.html' %}{% assign lang = 'zh' %}{% endif %}

* [Librera 8.1](/wiki/what-is-new/8.1/{{lang}})
* [Librera 8.0](/wiki/what-is-new/8.0/{{lang}})
* [Librera 7.12](/wiki/what-is-new/7.12/{{lang}})
* [Librera 7.11](/wiki/what-is-new/7.11/{{lang}})
* [Librera 7.10](/wiki/what-is-new/7.10/{{lang}})