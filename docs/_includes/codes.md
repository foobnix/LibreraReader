{% assign parent = '/' %}

{% assign mylast = page.dir | split: "/" | last | append: "/" %}
{% assign parent = page.dir | remove: mylast %}
