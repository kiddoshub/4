// Initialize particles.js
particlesJS('particles-js', {
  particles: {
    number: {
      value: 80,  // Number of particles
      density: {
        enable: true,
        value_area: 800  // Density of the particles
      }
    },
    color: { value: "#ffffff" },  // Particle color
    shape: {
      type: "circle",  // Shape of particles
    },
    opacity: {
      value: 0.5,  // Opacity of particles
      anim: {
        enable: true,
        speed: 1,
        opacity_min: 0.1
      }
    },
    size: {
      value: 3,  // Size of particles
      random: true,  // Random size
      anim: {
        enable: true,
        speed: 40,
        size_min: 0.1
      }
    },
    move: {
      enable: true,
      speed: 6,  // Speed of particles
      direction: "none",
      random: false,
      straight: false,
      out_mode: "out",  // Particles will leave the screen
      bounce: false
    }
  },
  interactivity: {
    detect_on: "canvas",
    events: {
      onhover: {
        enable: true,
        mode: "repulse"  // Repulse particles on hover
      },
      onclick: {
        enable: true,
        mode: "push"  // Push particles on click
      }
    }
  },
  retina_detect: true  // Ensure particles are displayed on retina displays
});
