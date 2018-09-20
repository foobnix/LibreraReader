---
layout: main
---
![](/css/logo-line.jpg)

# Menu
  
## English
{% capture my_include %}{% include en-left-menu.md %}{% endcapture %}
{{ my_include | markdownify }}
    
## Русский
{% capture my_include %}{% include ru-left-menu.md %}{% endcapture %}
{{ my_include | markdownify }}