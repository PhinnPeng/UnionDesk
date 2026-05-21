import type { IconifyJSON } from "@iconify/react/offline";

import { addCollection } from "@iconify/react/offline";

import antDesignCollection from "./iconify-offline-data/ant-design.json";
import carbonCollection from "./iconify-offline-data/carbon.json";
import fa6SolidCollection from "./iconify-offline-data/fa6-solid.json";
import heroiconsCollection from "./iconify-offline-data/heroicons.json";
import lucideCollection from "./iconify-offline-data/lucide.json";
import mdiCollection from "./iconify-offline-data/mdi.json";
import riCollection from "./iconify-offline-data/ri.json";
import tablerCollection from "./iconify-offline-data/tabler.json";

export interface IconCollectionSource {
	label: string
	value: string
	data: IconifyJSON
}

export interface IconCollectionPreset {
	label: string
	value: string
	iconNames: string[]
}

export interface IconPickerCatalog {
	collections: IconCollectionPreset[]
	collectionOptions: Array<{ label: string; value: string }>
	getCollectionIcons: (prefix: string) => string[]
	searchIcons: (keyword: string) => string[]
}

const PRESET_ICON_COLLECTION_SOURCES: IconCollectionSource[] = [
	{ label: "Ant Design", value: "ant-design", data: antDesignCollection },
	{ label: "Material Design Icons", value: "mdi", data: mdiCollection },
	{ label: "Lucide", value: "lucide", data: lucideCollection },
	{ label: "Heroicons", value: "heroicons", data: heroiconsCollection },
	{ label: "Tabler Icons", value: "tabler", data: tablerCollection },
	{ label: "Font Awesome 6", value: "fa6-solid", data: fa6SolidCollection },
	{ label: "Remix Icon", value: "ri", data: riCollection },
	{ label: "Carbon", value: "carbon", data: carbonCollection },
];

function collectIconNames(data: IconifyJSON) {
	const names = new Set<string>();

	for (const iconName of Object.keys(data.icons ?? {})) {
		names.add(iconName);
	}

	for (const aliasName of Object.keys(data.aliases ?? {})) {
		names.add(aliasName);
	}

	return Array.from(names).sort((left, right) => left.localeCompare(right));
}

export function createIconPickerCatalog(sources: readonly IconCollectionSource[]) {
	const collections = sources.map(source => ({
		label: source.label,
		value: source.value,
		iconNames: collectIconNames(source.data),
	}));
	const collectionMap = new Map(collections.map(collection => [collection.value, collection]));
	const searchIndex = collections.flatMap(collection =>
		collection.iconNames.map(name => ({
			value: `${collection.value}:${name}`,
			searchText: `${collection.label} ${collection.value} ${name}`.toLowerCase(),
		})),
	);

	return {
		collections,
		collectionOptions: collections.map(({ label, value }) => ({ label, value })),
		getCollectionIcons(prefix: string) {
			const icons = collectionMap.get(prefix)?.iconNames ?? [];
			return icons.map(name => `${prefix}:${name}`);
		},
		searchIcons(keyword: string) {
			const normalizedKeyword = keyword.trim().toLowerCase();
			if (normalizedKeyword.length < 2) {
				return [];
			}
			return searchIndex
				.filter(item => item.searchText.includes(normalizedKeyword))
				.map(item => item.value);
		},
	} satisfies IconPickerCatalog;
}

export const iconPickerCatalog = createIconPickerCatalog(PRESET_ICON_COLLECTION_SOURCES);

let collectionsRegistered = false;

export function registerPresetIconCollections() {
	if (collectionsRegistered) {
		return;
	}

	for (const source of PRESET_ICON_COLLECTION_SOURCES) {
		addCollection(source.data);
	}

	collectionsRegistered = true;
}

registerPresetIconCollections();

export const PRESET_ICON_COLLECTION_OPTIONS = iconPickerCatalog.collectionOptions;

export function getPresetCollectionIcons(prefix: string) {
	return iconPickerCatalog.getCollectionIcons(prefix);
}

export function searchPresetIcons(keyword: string) {
	return iconPickerCatalog.searchIcons(keyword);
}
