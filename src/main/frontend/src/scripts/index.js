const SIZE = 3;
const MAP_SIZE = 12;
const THREE = window.THREE;

function clearDomElement(element) {
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

class WorldRenderer3d {
  constructor(container) {
    this.container = container;
  }

  createScene() {
    const { width, height } = this.container.getBoundingClientRect();
    this.scene = new THREE.Scene();
    
    const aspect = width / height;
    const d = 20;
    this.camera = new THREE.OrthographicCamera( - d * aspect, d * aspect, d, - d, 1, 1000  );
    this.camera.position.set( 20, 20, 20 );
    this.camera.lookAt( this.scene.position );
    
    
    this.renderer = new THREE.WebGLRenderer();
    this.renderer.setClearColor(0xFFFFFF, 1);
    this.renderer.setSize( width, height );
    this.controls = new THREE.OrbitControls( this.camera, this.renderer.domElement );
    this.container.appendChild( this.renderer.domElement );
  }

  addLighting() {
    const hemisphere = new THREE.HemisphereLight(0xffffff, 0xffffff, 1);
    this.scene.add( hemisphere );

    const sun = new THREE.PointLight( 0xffffff, 0.8 );
    sun.position.set( 0, 50, 50 );
    this.scene.add( sun );
  }
  
  createMap() {
    const geometry = new THREE.PlaneGeometry( MAP_SIZE * SIZE, MAP_SIZE * SIZE, 32 );
    const material = new THREE.MeshBasicMaterial( {color: 0xffffff, side: THREE.DoubleSide} );
    const plane = new THREE.Mesh( geometry, material );
    plane.rotateX( - Math.PI / 2);

    this.scene.add( plane );
    this.scene.add( new THREE.AxisHelper( 40 ) );
  }

  render(data) {
    this.createScene();
    this.addLighting();
    this.createMap();
    
    const walls = data.staticObjects.filter(obj => obj.type === 'wall');
    this.createWalls(walls);
    this.createTanks(data.tanks);

    const trees = data.staticObjects.filter(obj => obj.type === 'tree');
    this.createTrees(trees);

    this.createLasers(data.lasers);

    this.animate();
  }

  convertFromGridToWold(x, y) {
    // WIDTH 1 in grid size is SIZE in threejs.
    // Then the center is at the center of the map instead of 0,0
    const worldX = (SIZE * y) - (SIZE * (0.5 * MAP_SIZE - 0.5));
    const worldZ = (SIZE * (0.5 * MAP_SIZE - 0.5)) - (SIZE * x);

    return [worldX, worldZ];
  }

  createWalls(walls) {
    walls.forEach(wall => {
      this.createWall(wall.position[0], wall.position[1]);
    });
  }

  createTanks(tanks) {
    const geometry = new THREE.BoxGeometry(SIZE, 2, SIZE);

    const material = new THREE.MeshStandardMaterial();
    material.color.setHex(0x1A560A);

    tanks.map(tank => {
      const [x, y] = tank.position;
      const mesh = new THREE.Mesh( geometry, material );
      this.scene.add( mesh );
      mesh.position.y = 1;
  
      const [worldX, worldZ] = this.convertFromGridToWold(x, y);
  
      mesh.position.x = worldX;
      mesh.position.z = worldZ;
    });
  }

  createTrees(trees) {
    const geometry = new THREE.BoxGeometry(1, 4, 1);

    const material = new THREE.MeshStandardMaterial();
    material.color.setHex(0x4E2D04);

    trees.map(tree => {
      const [x, y] = tree.position;
      const mesh = new THREE.Mesh( geometry, material );
      this.scene.add( mesh );
      mesh.position.y = 2;
  
      const [worldX, worldZ] = this.convertFromGridToWold(x, y);
  
      mesh.position.x = worldX;
      mesh.position.z = worldZ;
    });
  }

  createWall(x, y) {
    const geometry = new THREE.BoxGeometry( SIZE, 1, SIZE);

    const material = new THREE.MeshStandardMaterial();
    material.color.setHex(0xffffff);

    const mesh = new THREE.Mesh( geometry, material );
    this.scene.add( mesh );
    mesh.position.y = 0.5;

    const [worldX, worldZ] = this.convertFromGridToWold(x, y);

    mesh.position.x = worldX;
    mesh.position.z = worldZ;
  }

  createLasers(lasers) {
    const geometry = new THREE.BoxGeometry(3, 1, 1);

    const material = new THREE.MeshNormalMaterial();
    // material.color.setHex(0xFE69B4);

    lasers.forEach(laser => {
      const [xStart, yStart] = laser.startPos;
      this.createObjectAtPosition(geometry, material, xStart, yStart, 1.5);
      
      const [xEnd, yEnd] = laser.endPos;
      this.createObjectAtPosition(geometry, material, xEnd, yEnd, 1.5);

      const intermediatePositions = getIntermediatePositions(laser.startPos, laser.endPos);

      intermediatePositions.forEach(position => {
        this.createObjectAtPosition(geometry, material, position[0], position[1], 1.5);
      });
    });
  }

  createObjectAtPosition(geometry, material, x, y, height) {
    const [worldX, worldZ] = this.convertFromGridToWold(x, y);
    const mesh = new THREE.Mesh( geometry, material);

    mesh.position.y = height;
    mesh.position.x = worldX;
    mesh.position.z = worldZ;
    this.scene.add( mesh );
  }
  
  animate() {
    this.controls.update();
    window.requestAnimationFrame(() => this.animate());
    this.renderer.render(this.scene, this.camera );
  }
}

class PlayerListRenderer {
  constructor(domTarget) {
    this.domTarget = domTarget;
    clearDomElement(this.domTarget);
  }

  render(players) {
    players.forEach(player => {
      this.renderPlayer(player);
    });
  }

  renderPlayer(player) {
    const element = document.createElement('div');
    element.classList.add('player');

    const color = document.createElement('div');
    color.classList.add('color-indicator');
    color.setAttribute('style', `background-color: ${player.color}`);
    element.appendChild(color);

    const name = document.createElement('p');
    name.classList.add('player-name');
    name.innerHTML = player.name;
    element.appendChild(name);

    const life = document.createElement('p');
    life.classList.add('player-life');
    const hearts = new Array(player.energy).fill('&#10084;');
    life.innerHTML = hearts.join('');
    element.appendChild(life);

    this.domTarget.appendChild(element);
  }
}

function renderWorld(data) {
  const worldRenderer = new WorldRenderer3d(document.querySelector('.grid'));
  const playerRenderer = new PlayerListRenderer(document.querySelector('.player-list'));

  worldRenderer.render(data);
  playerRenderer.render(data.tanks);
}

async function tick() {
  const response = await fetch('/world');
  const data = await response.json();
  renderWorld(data);
}

function getValuesBetween(start, end) {
  const output = [];
  let value = start;

  while (value <= end) {
    output.push(value);
    value++;
  }
  return output;
}

function getIntermediatePositions(startPosition, endPosition) {
  const [startX, startY] = startPosition;
  const [endX, endY] = endPosition;
  const output = [];
  const xIntermediate = getValuesBetween(startX, endX);
  const yIntermediate = getValuesBetween(startY, endY);

  xIntermediate.forEach(x => {
    yIntermediate.forEach(y => {
      if (x === startX && y === startY || x === endX && y === endY ) {
        return;
      }
      output.push([x, y]);
    });
  });

  return output;
}

function startGameloop() {
  setInterval(tick, 100);
}

window.onload = () => {
  renderWorld(window._world);
};
