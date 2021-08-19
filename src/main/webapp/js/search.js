function search(){
    fetch('/trpg/search')
    .then(res => res.json())
    .then(json => console.log(json))
}