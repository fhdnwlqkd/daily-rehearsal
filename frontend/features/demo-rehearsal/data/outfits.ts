import type { DemoOutfit } from "../types";

export const demoOutfits = [
  {
    outfitId: "demo-presentation-navy",
    label: "네이비 프레젠테이션 수트",
    imageUrl: "/demo/outfits/presentation-navy.png",
    prompt:
      "matching navy tailored single-breasted blazer and trousers with a crisp white crew-neck inner shirt, polished professional presentation outfit",
    enhance: true,
    defaultOutfit: true,
  },
  {
    outfitId: "demo-presentation-gray",
    label: "라이트 그레이 수트",
    imageUrl: "/demo/outfits/presentation-gray.png",
    prompt:
      "matching light gray tailored blazer and trousers with a pale blue button-up shirt, bright confident professional presentation outfit",
    enhance: true,
    defaultOutfit: false,
  },
  {
    outfitId: "demo-presentation-black",
    label: "블랙 후드 & 베이지 치노",
    imageUrl: "/demo/outfits/presentation-black.png",
    prompt:
      "plain black pullover hoodie with straight-fit warm beige cotton chino pants, coordinated clean casual outfit",
    enhance: true,
    defaultOutfit: false,
  },
] as const satisfies readonly DemoOutfit[];
