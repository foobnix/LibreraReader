REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$REPO/docs/what-is-new"
mogrify -quality 85 -resize 600 *.png

cd "$REPO/docs"

if [ "$1" == "init" ]; then
bundle init
bundle add webrick
bundle add jekyll-watch
bundle add kramdown-parser-gfm
fi

jekyll serve
